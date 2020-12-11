package com.yhl.lanlink.interfaces

import com.yhl.lanlink.*

class IDiscoveryListenerImpl(private val lanLink: LanLink) : IDiscoveryListener.Stub() {
    override fun onServiceFound(serviceInfo: ServiceInfo) {
        lanLink.runOnUiThread {
            lanLink.getDiscoveryListener()?.onServiceFound(serviceInfo)
        }
    }

    override fun onDiscoveryStop(resultCode: Int) {
        lanLink.runOnUiThread {
            lanLink.getDiscoveryListener()?.onDiscoveryStop(resultCode)
        }
    }

    override fun onServiceLost(serviceInfo: ServiceInfo) {
        lanLink.runOnUiThread {
            lanLink.getDiscoveryListener()?.onServiceLost(serviceInfo)
        }
    }

    override fun onDiscoveryStart(resultCode: Int) {
        lanLink.runOnUiThread {
            lanLink.getDiscoveryListener()?.onDiscoveryStart(resultCode)
        }
    }
}

class IRegistrationListenerImpl(private val lanLink: LanLink) :
    IRegistrationListener.Stub() {
    override fun onServiceUnregistered(resultCode: Int) {
        lanLink.runOnUiThread {
            lanLink.getRegistrationListener()?.onServiceUnregistered(resultCode)
        }
    }

    override fun onServiceRegistered(resultCode: Int) {
        lanLink.runOnUiThread {
            lanLink.getRegistrationListener()?.onServiceRegistered(resultCode)
        }
    }
}

class IConnectionListenerImpl(private val lanLink: LanLink) : IConnectionListener.Stub() {
    override fun onConnect(serviceInfo: ServiceInfo, resultCode: Int) {
        lanLink.runOnUiThread {
            lanLink.getConnectionListener()?.onConnect(serviceInfo, resultCode)
        }
    }

    override fun onMessage(serviceInfo: ServiceInfo, msg: Msg?) {
        msg?.let {
            println("onMessage: ${msg.tag}")
            val codec = lanLink.getMessageCodec(msg.tag)
            val messageListener = lanLink.getMessageListener()
            if (codec != null && messageListener != null) {
                val data = codec.decodeInner(msg)
                lanLink.runOnUiThread {
                    messageListener.onMessage(serviceInfo, msg.tag, data)
                }
            }
        }
    }

    override fun onDisconnect(serviceInfo: ServiceInfo, resultCode: Int) {
        lanLink.runOnUiThread {
            lanLink.getConnectionListener()?.onDisconnect(serviceInfo, resultCode)
        }
    }
}
