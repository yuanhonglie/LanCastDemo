package com.yhl.lanlink.nsd

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import com.yhl.lanlink.*
import com.yhl.lanlink.channel.Channel
import com.yhl.lanlink.data.MediaType
import com.yhl.lanlink.data.ServiceInfo

class ServiceManager {
    private val TAG = ServiceManager::class.simpleName
    private lateinit var mNsdManager: NsdManager
    private var mWorkerThread: HandlerThread
    private lateinit var mWorkerHandler: WorkerHandler
    private val mUiHandler = MainHandler(Looper.getMainLooper())
    private val serviceMap = mutableMapOf<String, ServiceInfo>()
    private val mChannelMap = mutableMapOf<String, Channel>()

    var mRegistrationListener: RegistrationListener? = null
    var mDiscoveryListener: DiscoveryListener? = null
    var mConnectionListener: ConnectionListener? = null

    @Volatile
    private var discovering = false
    @Volatile
    private var registered = false

    private constructor(context: Context) {
        mNsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        mWorkerThread = initWorkerThread()
        mWorkerThread.start()
        mWorkerHandler = WorkerHandler(this, mWorkerThread.looper)
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
                    serviceManager.notifyServerDisconnected(channel.server, msg.arg1)
                }
                MSG_WORKER_SERVER_CONNECT -> {
                    val channel = msg.obj as Channel
                    serviceManager.notifyServerConnected(channel.server, msg.arg1)
                }
            }
        }
    }

    private inner class MainHandler(looper: Looper): Handler(looper)

    fun notifyServerConnected(serviceInfo: ServiceInfo, resultCode: Int) {
        runOnUiThread {
            mConnectionListener?.onConnect(serviceInfo, resultCode)
        }
    }

    fun notifyServerDisconnected(serviceInfo: ServiceInfo, resultCode: Int) {
        mChannelMap.remove(serviceInfo.id)
        runOnUiThread {
            mConnectionListener?.onDisconnect(serviceInfo, resultCode)
        }
    }

    /**
     * Register a service to be discovered by other services.
     */
    fun registerService(name: String) {
        runOnWorkerThread {
            if (registered.not()) {
                registered = true
                val serviceInfo = NsdServiceInfo().apply {
                    serviceName = name
                    serviceType = LINK_SERVICE_TYPE
                    port = MESSAGE_SERVER_PORT
                }
                println("registerService: ${serviceInfo}")
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
    fun unregisterService() {
        runOnWorkerThread {
            if (registered) {
                registered = false
                mNsdManager.unregisterService(registrationListener)
            }
        }
    }

    /**
     * 启动服务发现
     */
    fun startDiscovery() {
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
    fun stopDiscovery() {
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
    fun connect(serviceInfo: ServiceInfo) {
        if (mChannelMap.containsKey(serviceInfo.id)) {
            println("You have already connected to this server")
        } else {
            val channel = Channel(mWorkerHandler, serviceInfo)
            mChannelMap[serviceInfo.id] = channel
            channel.connect()
        }
    }

    /**
     * 断开服务
     */
    fun disconnect(serviceInfo: ServiceInfo) {
        if (mChannelMap.containsKey(serviceInfo.id)) {
            val channel = mChannelMap[serviceInfo.id]
            channel?.disconnect()
        } else {
            println("You can not disconnect a server unless you have connected to this server")
        }
    }

    fun sendCastTask(channel: Channel, uri: String, type: MediaType) {
        channel.sendCastTask(uri, type)
    }

    private fun runOnWorkerThread(r: () -> Unit) {
        mWorkerHandler?.post(r)
    }

    private fun runOnUiThread(r: () -> Unit) {
        mUiHandler.post(r)
    }

    fun destroy() {
        serviceMap.clear()
        mRegistrationListener = null
        mDiscoveryListener = null
        mConnectionListener = null
        mWorkerThread.quit()
    }

    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            println("onUnregistrationFailed: errorCode = $errorCode")
            runOnUiThread {
                mRegistrationListener?.onServiceUnregistered(SERVICE_RESULT_FAILED)
            }
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
            println("onServiceUnregistered")
            runOnUiThread {
                mRegistrationListener?.onServiceUnregistered(SERVICE_RESULT_SUCCESS)
            }
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            println("onRegistrationFailed: errorCode = ${errorCode}")
            runOnUiThread {
                mRegistrationListener?.onServiceRegistered(SERVICE_RESULT_FAILED)
            }
        }

        override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
            println("onServiceRegistered:")
            runOnUiThread {
                mRegistrationListener?.onServiceRegistered(SERVICE_RESULT_SUCCESS)
            }
        }
    }

    private val discoveryListener = object: NsdManager.DiscoveryListener {

        override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
            println("onServiceFound: ${serviceInfo}")
            if (serviceInfo != null) {
                mNsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                        println("onResolveFailed ${serviceInfo}")
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
                        println("onServiceFound -> onServiceResolved ${serviceInfo}")
                        if (serviceInfo != null) {
                            runOnUiThread {
                                val sInfo = ServiceInfo(
                                    serviceInfo.serviceName,
                                    serviceInfo.host,
                                    serviceInfo.port
                                )
                                serviceMap.put(sInfo.id, sInfo)
                                mDiscoveryListener?.onServiceFound(sInfo)
                            }
                        }
                    }
                })
            }
        }

        override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
            println("onStopDiscoveryFailed: errorCode = ${errorCode}")
            runOnUiThread {
                mDiscoveryListener?.onDiscoveryStop(SERVICE_RESULT_FAILED)
            }
        }

        override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
            println("onStartDiscoveryFailed: errorCode = ${errorCode}")
            runOnUiThread {
                mDiscoveryListener?.onDiscoveryStart(SERVICE_RESULT_FAILED)
            }
        }

        override fun onDiscoveryStarted(serviceType: String?) {
            println("onDiscoveryStarted:")
            runOnUiThread {
                mDiscoveryListener?.onDiscoveryStart(SERVICE_RESULT_SUCCESS)
            }
        }

        override fun onDiscoveryStopped(serviceType: String?) {
            println("onDiscoveryStopped:")
            runOnUiThread {
                mDiscoveryListener?.onDiscoveryStop(SERVICE_RESULT_SUCCESS)
            }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
            println("onServiceLost:${serviceInfo}")
            if (serviceInfo != null) {
                mNsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) { }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
                        println("onServiceLost -> onServiceResolved ${serviceInfo}")
                        runOnUiThread {
                            if (serviceInfo != null) {
                                val sInfo = ServiceInfo(
                                    serviceInfo.serviceName,
                                    serviceInfo.host,
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
        fun getInstance(c: Context): ServiceManager {
            if (instance == null) {
                synchronized(ServiceManager::class) {
                    if (instance == null) {
                        instance = ServiceManager(c)
                    }
                }
            }
            return instance!!
        }
    }
}

const val SERVICE_RESULT_SUCCESS = 0
const val SERVICE_RESULT_FAILED = -1
interface RegistrationListener {
    /**
     * 服务注册事件回调
     * @param resultCode {@link SERVICE_RESULT_SUCCESS} ：注册成功，
     *                  <br>{@link SERVICE_RESULT_FAILED} ：注册失败
     */
    fun onServiceRegistered(resultCode: Int)

    /**
     * 服务注销事件回调
     * @param resultCode {@link SERVICE_RESULT_SUCCESS} ：注册成功，
     *                  <br>{@link SERVICE_RESULT_FAILED} ：注册失败
     */
    fun onServiceUnregistered(resultCode: Int)
}

interface DiscoveryListener {
    /**
     * 开始发现服务事件回调
     * @param resultCode {@link SERVICE_RESULT_SUCCESS} ：注册成功，
     *                  <br>{@link SERVICE_RESULT_FAILED} ：注册失败
     */
    fun onDiscoveryStart(resultCode: Int)

    /**
     * 停止发现服务事件回调
     * @param resultCode {@link SERVICE_RESULT_SUCCESS} ：注册成功，
     *                  <br>{@link SERVICE_RESULT_FAILED} ：注册失败
     */
    fun onDiscoveryStop(resultCode: Int)

    /**
     * 发现服务回调
     * @param serviceInfo 被发现的服务信息，一个{@link ServiceInfo}实例
     */
    fun onServiceFound(serviceInfo: ServiceInfo)

    /**
     * 服务丢失回调
     * @param serviceInfo 丢失的服务信息，一个{@link ServiceInfo}实例
     */
    fun onServiceLost(serviceInfo: ServiceInfo)
}

interface ConnectionListener {
    /**
     * 连接服务事件
     */
    fun onConnect(serviceInfo: ServiceInfo, resultCode: Int)

    /**
     * 断开服务事件
     */
    fun onDisconnect(serviceInfo: ServiceInfo, resultCode: Int)
}