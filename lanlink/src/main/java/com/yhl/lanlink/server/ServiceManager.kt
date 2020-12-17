package com.yhl.lanlink.server

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.*
import com.yhl.lanlink.*
import com.yhl.lanlink.channel.Channel
import com.yhl.lanlink.log.Logger
import com.yhl.lanlink.util.getIPv4Address

class ServiceManager private constructor(private val service: HttpService) :
    ILanLinkService.Stub() {
    private val TAG = "ServiceManager"
    private var mNsdManager: NsdManager = service.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var mWorkerThread: HandlerThread
    private val serviceMap = mutableMapOf<String, ServiceInfo>()
    private val clientMap = mutableMapOf<String, ServiceInfo>()
    private val mChannelMap = mutableMapOf<String, Channel>()
    private var mRegistrationListener: IRegistrationListener? = null

    private var mDiscoveryListener: IDiscoveryListener? = null
    private var mConnectionListener: IConnectionListener? = null
    @Volatile
    private var discovering = false

    @Volatile
    private var registered = false

    val mConnectionManager = ConnectionManager(this)
    var mWorkerHandler: WorkerHandler

    init {
        mWorkerThread = initWorkerThread()
        mWorkerThread.start()
        mWorkerHandler = WorkerHandler(
            this,
            mWorkerThread.looper
        )
    }

    private fun initWorkerThread() = HandlerThread("service-manager")

    class WorkerHandler(private val serviceManager: ServiceManager, looper: Looper): Handler(looper) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when(msg.what) {
                MSG_WORKER_HEART_BEAT -> {
                    val channel = msg.obj as Channel
                    if (channel.isActive) {
                        channel.heartbeat()
                    }
                }
                MSG_WORKER_SERVER_DISCONNECT -> {
                    val channel = msg.obj as Channel
                    serviceManager.notifyServerDisconnected(channel.mServer, msg.arg1)
                }
                MSG_WORKER_SERVER_CONNECT -> {
                    val channel = msg.obj as Channel
                    serviceManager.notifyServerConnected(channel.mServer, msg.arg1)
                }
                MSG_WORKER_CHECK_CLIENT_TIMEOUT -> {
                    serviceManager.performClientAliveCheck()
                }
            }
        }
    }

    /**
     * 作为客户端，连接上目标服务端之后，告知应用层，当前已连接上服务端
     */
    fun notifyServerConnected(serviceInfo: ServiceInfo, resultCode: Int) {
        mConnectionListener?.onConnect(serviceInfo, resultCode)
    }

    /**
     * 作为客户端，断开目标客户端之后，告知应用层，当前与某个服务端断开连接
     */
    fun notifyServerDisconnected(serviceInfo: ServiceInfo, resultCode: Int) {
        mChannelMap.remove(serviceInfo.id)
        mConnectionListener?.onDisconnect(serviceInfo, resultCode)
    }

    /**
     * 作为服务端，客户端连接到当前服务端之后，告知APP，当前有客户端接入
     */
    fun notifyClientConnected(clientInfo: ServiceInfo, resultCode: Int) {
        performClientAliveCheck()
        clientMap[clientInfo.id] = clientInfo
        mConnectionListener?.onConnect(clientInfo, resultCode)
    }

    /**
     * 作为服务端，当有客户端断开后，告知APP，有客户端已经断开
     */
    fun notifyClientDisconnected(clientInfo: ServiceInfo, resultCode: Int) {
        clientMap.remove(clientInfo.id)
        mConnectionListener?.onDisconnect(clientInfo, resultCode)
    }

    fun performClientAliveCheck() {
        if (mConnectionManager.performClientsAliveCheck()) {
            mWorkerHandler.removeMessages(MSG_WORKER_CHECK_CLIENT_TIMEOUT)
            val msg = mWorkerHandler.obtainMessage(MSG_WORKER_CHECK_CLIENT_TIMEOUT)
            mWorkerHandler.sendMessageDelayed(msg, INTERVAL_HEART_BEAT)
        } else {
            mWorkerHandler.removeMessages(MSG_WORKER_CHECK_CLIENT_TIMEOUT)
        }
    }

    /**
     * Register a service to be discovered by other services.
     */
    override fun registerService(name: String) {
        runOnWorkerThread {
            if (registered.not()) {
                registered = true
                val serviceInfo = NsdServiceInfo().apply {
                    serviceName = name
                    serviceType = LINK_SERVICE_TYPE
                    port = MESSAGE_SERVER_PORT
                }
                Logger.i(TAG, "registerService: ${serviceInfo}")
                mNsdManager.registerService(
                    serviceInfo,
                    NsdManager.PROTOCOL_DNS_SD,
                    registrationListener
                )
            }
        }

    }

    /**
     * Unregister a service registered through {@link #registerService}
     */
    override fun unregisterService() {
        runOnWorkerThread {
            if (registered) {
                registered = false
                mNsdManager.unregisterService(registrationListener)
            }
        }
    }

    override fun asBinder(): IBinder? = null

    /**
     * 启动服务发现
     */
    override fun startDiscovery() {
        runOnWorkerThread {
            if (discovering.not()) {
                discovering = true
                mNsdManager.discoverServices(LINK_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
            }
        }
    }

    /**
     * 停止发现服务
     */
    override fun stopDiscovery() {
        runOnWorkerThread {
            if (discovering) {
                discovering = false
                mNsdManager.stopServiceDiscovery(discoveryListener)
            }
        }
    }

    /**
     * 连接服务
     */
    override fun connect(serviceId: String) {
        if (mChannelMap.containsKey(serviceId)) {
            Logger.i(TAG, "You have already connected to this server")
        } else {
            val serviceInfo = serviceMap[serviceId]
            if (serviceInfo != null) {
                val channel = Channel(mConnectionManager, mWorkerHandler, serviceInfo)
                mChannelMap[serviceInfo.id] = channel
                channel.connect()
            }
        }
    }

    /**
     * 断开服务
     */
    override fun disconnect(serviceId: String) {
        if (mChannelMap.containsKey(serviceId)) {
            val channel = mChannelMap[serviceId]
            channel?.disconnect()
        } else {
            Logger.i(TAG, "You can not disconnect a server unless you have connected to this server")
        }
    }

    override fun setRegistrationListener(listener: IRegistrationListener?) {
        mRegistrationListener = listener
    }

    override fun setDiscoveryListener(listener: IDiscoveryListener?) {
        mDiscoveryListener = listener
    }

    override fun setConnectionListener(listener: IConnectionListener?) {
        mConnectionListener = listener
    }

    override fun send(serviceId: String?, msg: Msg?) {
        val serviceInfo = if (serviceMap.containsKey(serviceId)) {
            val server = serviceMap[serviceId]
            Logger.i(TAG, "send message to server ${server?.host}")
            server
        } else if (clientMap.containsKey(serviceId)) {
            val client = clientMap[serviceId]
            Logger.i(TAG, "send message to client ${client?.host}")
            client
        } else {
            Logger.i(TAG, "can not find serviceInfo id = $serviceId")
            null
        }

        Logger.i(TAG, "send: $serviceInfo, $msg")
        if (msg != null) {
            serviceInfo?.sendMessage(msg)
        }
    }

    override fun serveFile(path: String): String {
        val encoded = service.getServeFilePath(path)
        return "${getFileServerUrl()}/$encoded"
    }

    private fun getFileServerUrl() = "http://${getIPv4Address()}:$FILE_SERVER_PORT"

    fun onReceiveMessage(serviceInfo: ServiceInfo, msg: Msg) {
        mConnectionListener?.onMessageReceive(serviceInfo, msg)
    }

    private fun runOnWorkerThread(r: () -> Unit) {
        mWorkerHandler.post(r)
    }

    private fun runOnUiThread(r: () -> Unit) {
        r.invoke()
    }

    override fun destroy() {
        Logger.i(TAG, "destroy: ")
    }

    fun onDestroy() {
        stopDiscovery()
        mConnectionManager.destroy()
        serviceMap.clear()
        clientMap.clear()
        mRegistrationListener = null
        mDiscoveryListener = null
        mConnectionListener = null
        mWorkerThread.quit()
        instance = null
    }

    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            Logger.i(TAG, "onUnregistrationFailed: errorCode = $errorCode")
            runOnUiThread {
                mRegistrationListener?.onServiceUnregistered(RESULT_FAILED)
            }
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
            Logger.i(TAG, "onServiceUnregistered")
            runOnUiThread {
                mRegistrationListener?.onServiceUnregistered(RESULT_SUCCESS)
            }
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            Logger.i(TAG, "onRegistrationFailed: errorCode = ${errorCode}")
            runOnUiThread {
                mRegistrationListener?.onServiceRegistered(RESULT_FAILED)
            }
        }

        override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
            Logger.i(TAG, "onServiceRegistered:")
            runOnUiThread {
                mRegistrationListener?.onServiceRegistered(RESULT_SUCCESS)
            }
        }
    }

    private val discoveryListener = object: NsdManager.DiscoveryListener {

        override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
            Logger.i(TAG, "onServiceFound: ${serviceInfo}")
            if (serviceInfo != null) {
                mNsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                        Logger.i(TAG, "onResolveFailed ${serviceInfo}")
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
                        Logger.i(TAG, "onServiceFound -> onServiceResolved ${serviceInfo}")
                        if (serviceInfo != null) {
                            runOnUiThread {
                                val sInfo = ServiceInfo(
                                    serviceInfo.serviceName,
                                    serviceInfo.host.hostAddress,
                                    serviceInfo.port
                                )
                                if (serviceMap.containsKey(sInfo.id)) {
                                    val cached = serviceMap[sInfo.id]
                                    mDiscoveryListener?.onServiceFound(cached)
                                } else {
                                    serviceMap.put(sInfo.id, sInfo)
                                    mDiscoveryListener?.onServiceFound(sInfo)
                                }
                            }
                        }
                    }
                })
            }
        }

        override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
            Logger.i(TAG, "onStopDiscoveryFailed: errorCode = ${errorCode}")
            runOnUiThread {
                mDiscoveryListener?.onDiscoveryStop(RESULT_FAILED)
            }
        }

        override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
            Logger.i(TAG, "onStartDiscoveryFailed: errorCode = ${errorCode}")
            runOnUiThread {
                mDiscoveryListener?.onDiscoveryStart(RESULT_FAILED)
            }
        }

        override fun onDiscoveryStarted(serviceType: String?) {
            Logger.i(TAG, "onDiscoveryStarted:")
            runOnUiThread {
                mDiscoveryListener?.onDiscoveryStart(RESULT_SUCCESS)
            }
        }

        override fun onDiscoveryStopped(serviceType: String?) {
            Logger.i(TAG, "onDiscoveryStopped:")
            runOnUiThread {
                mDiscoveryListener?.onDiscoveryStop(RESULT_SUCCESS)
            }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
            Logger.i(TAG, "onServiceLost:${serviceInfo}")
            if (serviceInfo != null) {
                mNsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) { }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
                        Logger.i(TAG, "onServiceLost -> onServiceResolved ${serviceInfo}")
                        runOnUiThread {
                            if (serviceInfo != null) {
                                val sInfo = ServiceInfo(
                                    serviceInfo.serviceName,
                                    serviceInfo.host.hostAddress,
                                    serviceInfo.port
                                )
                                val cached = serviceMap.get(sInfo.id)
                                mDiscoveryListener?.onServiceLost(cached ?: sInfo)
                            }
                        }
                    }
                })
            }
        }
    }

    companion object {
        @Volatile
        private var instance: ServiceManager? = null
        fun getInstance(s: HttpService): ServiceManager {
            if (instance == null) {
                synchronized(ServiceManager::class) {
                    if (instance == null) {
                        instance = ServiceManager(s)
                    }
                }
            }
            return instance!!
        }
    }
}