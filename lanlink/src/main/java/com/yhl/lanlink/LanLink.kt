package com.yhl.lanlink

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.*
import com.yhl.lanlink.data.ActionType
import com.yhl.lanlink.data.ControlInfoCodec
import com.yhl.lanlink.data.MediaType
import com.yhl.lanlink.data.TaskInfoCodec
import com.yhl.lanlink.interfaces.*
import com.yhl.lanlink.server.HttpService
import java.lang.ref.WeakReference

class LanLink: ILinkReceiver, ILinkSender {

    private var service: ILanLinkService? = null
    private val uiHandler = Handler(Looper.getMainLooper())

    private var messageCodecs = mutableMapOf<String, MessageCodec>()

    private var initializeListener: WeakReference<InitializeListener?>? = null
    private var connectionListener: WeakReference<ConnectionListener?>? = null
    private var messageListener: WeakReference<MessageListener?>? = null
    private var registrationListener: WeakReference<RegistrationListener?>? = null
    private var discoveryListener: WeakReference<DiscoveryListener?>? = null

    @Volatile
    private var initialized: Boolean = false

    override fun isInitialized() = initialized

    override fun setInitializeListener(listener: InitializeListener?) {
        initializeListener = if (listener == null) null else WeakReference(listener)
        println("setInitializeListener: $listener, isInitialized = ${isInitialized()}")
        if (isInitialized()) {
            listener?.onInitialized()
        }
    }

    override fun setDiscoveryListener(listener: DiscoveryListener?) {
        discoveryListener = if (listener == null) null else WeakReference(listener)
    }

    fun getDiscoveryListener() = discoveryListener?.getInstance()

    override fun setConnectionListener(listener: ConnectionListener?) {
        connectionListener = if (listener == null) null else WeakReference(listener)
    }

    fun getConnectionListener() = connectionListener?.getInstance()

    override fun setRegistrationListener(listener: RegistrationListener?) {
        registrationListener = if (listener == null) null else WeakReference(listener)
    }

    fun getRegistrationListener() = registrationListener?.getInstance()

    override fun setMessageListener(listener: MessageListener?) {
        messageListener = if (listener == null) null else WeakReference(listener)
    }

    fun getMessageListener() = messageListener?.getInstance()

    private constructor(context: Context) {
        val connection = object: ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val service = ILanLinkService.Stub.asInterface(binder)
                println("onServiceConnected: $service")
                service?.let {
                    onInitialized(it)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                initialized = false
            }
        }
        val intent = Intent(context, HttpService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        registerMessageCodec(TaskInfoCodec())
        registerMessageCodec(ControlInfoCodec())
    }

    private fun onInitialized(service: ILanLinkService) {
        println("onInitialized: ")
        this.service = service
        service.setRegistrationListener(IRegistrationListenerImpl(this))
        service.setDiscoveryListener(IDiscoveryListenerImpl(this))
        service.setConnectionListener(IConnectionListenerImpl(this))
        initializeListener?.getInstance()?.onInitialized()
        initialized = true
    }

    override fun registerService(name: String) {
        service?.registerService(name)
    }

    override fun unregisterService() {
        service?.unregisterService()
    }

    override fun startDiscovery() {
        println("startDiscovery")
        service?.startDiscovery()
    }

    override fun stopDiscovery() {
        println("stopDiscovery")
        service?.stopDiscovery()
    }

    override fun connect(serviceInfo: ServiceInfo) {
        service?.connect(serviceInfo.id)
    }

    override fun disconnect(serviceInfo: ServiceInfo) {
        service?.disconnect(serviceInfo.id)
    }

    override fun registerMessageCodec(codec: MessageCodec) {
        if (messageCodecs.containsKey(codec.getMessageType())) {
            throw RuntimeException("You have register a codec with the same message type \"${codec.getMessageType()}\"")
        } else {
            messageCodecs.put(codec.getMessageType(), codec)
        }
    }

    fun getMessageCodec(tag: String) = messageCodecs[tag]

    override fun sendCastTask(serviceInfo: ServiceInfo, uri: String, mediaType: MediaType, actionType: ActionType) {
        service?.sendCastTask(serviceInfo.id, uri, mediaType.toString(), actionType.toString())
    }

    override fun sendCastExit(serviceInfo: ServiceInfo) {
        service?.sendCastExit(serviceInfo.id)
    }

    override fun sendMessage(serviceInfo: ServiceInfo, msg: Any, tag: String?) {
        val type = tag ?: msg::class.qualifiedName
        val codec = messageCodecs[type]
        if (codec != null) {
            service?.send(serviceInfo.id, codec.encodeInner(msg))
        }
    }

    override fun sendMessage(serviceInfo: ServiceInfo, msg: Any) {
        sendMessage(serviceInfo, msg, null)
    }

    override fun serveFile(path: String?): String {
        return if (path.isNullOrBlank()) {
            ""
        } else {
            service?.serveFile(path) ?: ""
        }
    }

    override fun serveFile(uri: Uri?): String {
        return if (uri == null) {
            ""
        } else {
            service?.serveFile(uri.toString()) ?: ""
        }
    }

    fun runOnUiThread(r: () -> Unit) {
        uiHandler.post(r)
    }

    private fun destroy() {
        uiHandler.removeCallbacksAndMessages(null)
        service?.destroy()
    }

    companion object {
        @Volatile
        private var instance: LanLink? = null
        fun initialize(c: Context): Boolean {
            if (instance == null) {
                synchronized(LanLink::class) {
                    if (instance == null) {
                        instance = LanLink(c.applicationContext)
                    }
                }
            }
            return instance != null
        }

        fun getInstance(): LanLink {
            if (instance == null) {
                throw RuntimeException("you should call initialize() first!!")
            }
            return instance!!
        }
    }
}

fun <T> WeakReference<T>.getInstance(): T? {
    return this?.get() ?: null
}