package com.yhl.lanlink

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import com.yhl.lanlink.data.ActionType
import com.yhl.lanlink.data.MediaType
import com.yhl.lanlink.interfaces.*
import com.yhl.lanlink.server.HttpService

class LanLink: ILanLink {

    private var service: ILanLinkService? = null
    var initializeListener: InitializeListener? = null
        set(value) {
            field = value
            if (isInitialized) {
                initializeListener?.onInitialized()
            }
        }

    var connectionListener: ConnectionListener? = null
    var messageListener: MessageListener? = null
    var registrationListener: RegistrationListener? = null
    var discoveryListener: DiscoveryListener? = null

    var messageCodecs = mutableMapOf<String, MessageCodec>()
        private set(value) {
            field = value
        }

    @Volatile
    var isInitialized: Boolean = false
    private set(value) {
        field = value
    }

    var uiHandler = Handler(Looper.getMainLooper())

    private constructor(context: Context) {
        val connection = object: ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val service = ILanLinkService.Stub.asInterface(binder)
                service?.let {
                    onInitialized()
                }
                this@LanLink.service = service

            }

            override fun onServiceDisconnected(name: ComponentName?) {
                isInitialized = false
            }
        }
        val intent = Intent(context, HttpService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun onInitialized() {
        println("onInitialized: ")
        service?.let {
            it.setRegistrationListener(IRegistrationListenerImpl(this))
            it.setDiscoveryListener(IDiscoveryListenerImpl(this))
            it.setConnectionListener(IConnectionListenerImpl(this))
        }
        initializeListener?.onInitialized()
        isInitialized = true
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

    override fun setClientMessenger(messenger: Messenger) {
        println("setClientMessenger: $service -> $messenger")
        service?.setClientMessenger(messenger)
    }

    override fun registerMessageCodec(codec: MessageCodec) {
        if (messageCodecs.containsKey(codec.getMessageType())) {
            throw RuntimeException("You have register a codec with the same message type \"${codec.getMessageType()}\"")
        } else {
            messageCodecs.put(codec.getMessageType(), codec)
        }
    }

    override fun sendCastTask(serviceInfo: ServiceInfo, uri: String, mediaType: MediaType, actionType: ActionType) {
        service?.sendCastTask(serviceInfo.id, uri, mediaType.toString(), actionType.toString())
    }

    override fun sendCastExit(serviceInfo: ServiceInfo) {
        service?.sendCastExit(serviceInfo.id)
    }

    override fun sendMessage(serviceInfo: ServiceInfo, msg: Any) {
        val tag = msg::class.qualifiedName
        val codec = messageCodecs[tag]
        if (codec != null) {
            service?.send(serviceInfo.id, codec.encodeInner(msg))
        }
    }

    fun onMessage(serviceInfo: ServiceInfo, type: String, data: Any) {
        runOnUiThread {
            messageListener?.onMessage(serviceInfo, type, data)
        }
    }

    private fun runOnUiThread(r: () -> Unit) {
        uiHandler.post(r)
    }

    override fun destroy() {
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
                throw RuntimeException("you shoud call initialize() first!!")
            }
            return instance!!
        }
    }
}

interface InitializeListener {
    fun onInitialized()
}