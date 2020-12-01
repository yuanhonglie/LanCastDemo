package com.yhl.lanlink.nsd

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.Log
import com.yhl.lanlink.LINK_SERVICE_TYPE
import com.yhl.lanlink.MESSAGE_SERVER_PORT
import com.yhl.lanlink.data.ServiceInfo
import com.yhl.lanlink.util.getIPv4Address
import java.net.InetAddress

class ServiceManager {
    private val TAG = ServiceManager::class.simpleName
    private lateinit var mNsdManager: NsdManager
    private var mWorkerThread: HandlerThread
    private var mWorkerHandler: WorkerHandler? = null
    private var mUiHandler = MainHandler(Looper.getMainLooper())

    var mRegistrationListener: RegistrationListener? = null
    var mDiscoveryListener: DiscoveryListener? = null

    var discovering = false

    private constructor(context: Context) {
        mNsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        mWorkerThread = initWorkerThread()
        mWorkerThread.start()
        mWorkerHandler = WorkerHandler(mWorkerThread.looper)
    }

    private fun initWorkerThread() = HandlerThread("service-manager")

    private inner class WorkerHandler(looper: Looper): Handler(looper) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
        }
    }

    private inner class MainHandler(looper: Looper): Handler(looper) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

        }
    }

    /**
     * Register a service to be discovered by other services.
     */
    fun registerService(name: String) {
        mWorkerHandler?.post {
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = name
                serviceType = LINK_SERVICE_TYPE
                port = MESSAGE_SERVER_PORT
                host = InetAddress.getByName(getIPv4Address())
            }
            println("registerService: ${serviceInfo}")
            mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
        }
    }

    /**
     * Unregister a service registered through {@link #registerService}
     */
    fun unregisterService() {
        mWorkerHandler?.post {
            mNsdManager.unregisterService(registrationListener)
        }
    }

    /**
     * 启动服务发现
     */
    fun startDiscovery() {
        mWorkerHandler?.post {
            if (!discovering) {
                discovering = true
                mNsdManager.discoverServices(LINK_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
            }
        }
    }

    /**
     * 停止发现服务
     */
    fun stopDiscovery() {
        mWorkerHandler?.post {
            if (discovering) {
                discovering = false
                mNsdManager.stopServiceDiscovery(discoveryListener)
            }
        }
    }

    fun destroy() {
        mRegistrationListener = null
        mDiscoveryListener = null
        mWorkerThread.quit()
    }

    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            println("onUnregistrationFailed: errorCode = ${errorCode}")
            mUiHandler.post {
                mRegistrationListener?.onServiceUnregistered(SERVICE_RESULT_FAILED)
            }
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
            println("onServiceUnregistered")
            mUiHandler.post {
                mRegistrationListener?.onServiceUnregistered(SERVICE_RESULT_SUCCESS)
            }
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            println("onRegistrationFailed: errorCode = ${errorCode}")
            mUiHandler.post {
                mRegistrationListener?.onServiceRegistered(SERVICE_RESULT_FAILED)
            }
        }

        override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
            println("onServiceRegistered:")
            mUiHandler.post {
                mRegistrationListener?.onServiceRegistered(SERVICE_RESULT_SUCCESS)
            }
        }
    }


    private val discoveryListener = object: NsdManager.DiscoveryListener {
        override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
            println("onServiceFound: ${serviceInfo}")
            if (serviceInfo != null) {
                mNsdManager.resolveService(serviceInfo, resolveListener)
            }

        }

        override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
            println("onStopDiscoveryFailed: errorCode = ${errorCode}")
            mUiHandler.post {
                mDiscoveryListener?.onDiscoveryStop(SERVICE_RESULT_FAILED)
            }
        }

        override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
            println("onStartDiscoveryFailed: errorCode = ${errorCode}")
            mUiHandler.post {
                mDiscoveryListener?.onDiscoveryStart(SERVICE_RESULT_FAILED)
            }
        }

        override fun onDiscoveryStarted(serviceType: String?) {
            println("onDiscoveryStarted:")
            mUiHandler.post {
                mDiscoveryListener?.onDiscoveryStart(SERVICE_RESULT_SUCCESS)
            }
        }

        override fun onDiscoveryStopped(serviceType: String?) {
            println("onDiscoveryStopped:")
            mUiHandler.post {
                mDiscoveryListener?.onDiscoveryStop(SERVICE_RESULT_SUCCESS)
            }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
            println("onServiceLost:")
            mUiHandler.post {
                if (serviceInfo != null) {
                    val sInfo = ServiceInfo(serviceInfo.serviceName, serviceInfo.host, serviceInfo.port)
                    mDiscoveryListener?.onServiceLost(sInfo)
                }
            }
        }
    }

    private val resolveListener = object: NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            println("onResolveFailed ${serviceInfo}")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
            if (serviceInfo != null) {
                mUiHandler.post {
                    val sInfo = ServiceInfo(serviceInfo.serviceName, serviceInfo.host, serviceInfo.port)
                    mDiscoveryListener?.onServiceFound(sInfo)
                }
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