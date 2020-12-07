package com.yhl.lanlink

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
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

    var messageListener: MessageListener? = null
    private val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            messageListener?.onMessage(msg)
        }
    }
    private val clientMessenger = Messenger(handler)

    @Volatile
    var isInitialized: Boolean = false
    private set(value) {
        field = value
    }

    private constructor(context: Context) {
        val connection = object: ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                service = ILanLinkService.Stub.asInterface(binder)
                onInitialized()
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
        initializeListener?.onInitialized()
        isInitialized = true
        setClientMessenger(clientMessenger)
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

    override fun setRegistrationListener(listener: RegistrationListener?) {
        service?.setRegistrationListener(IRegistrationListenerImpl(listener))
    }

    override fun setDiscoveryListener(listener: DiscoveryListener?) {
        service?.setDiscoveryListener(IDiscoveryListenerImpl(listener))
    }

    override fun setConnectionListener(listener: ConnectionListener?) {
        service?.setConnectionListener(IConnectionListenerImpl(listener))
    }

    override fun sendCastTask(serviceInfo: ServiceInfo, uri: String, mediaType: MediaType) {
        service?.sendCastTask(serviceInfo.id, uri, mediaType.toString())
    }

    override fun sendCastExit(serviceInfo: ServiceInfo) {
        service?.sendCastExit(serviceInfo.id)
    }

    override fun destroy() {
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

interface MessageListener {
    fun onMessage(message: Message)
}