package com.yhl.lanlink.interfaces

import com.yhl.lanlink.*

class IDiscoveryListenerImpl(private val lanLink: LanLink) : IDiscoveryListener.Stub() {
    override fun onServiceFound(serviceInfo: ServiceInfo) {
        lanLink.runOnUiThread {
            lanLink.discoveryListener?.onServiceFound(serviceInfo)
        }
    }

    override fun onDiscoveryStop(resultCode: Int) {
        lanLink.runOnUiThread {
            lanLink.discoveryListener?.onDiscoveryStop(resultCode)
        }
    }

    override fun onServiceLost(serviceInfo: ServiceInfo) {
        lanLink.runOnUiThread {
            lanLink.discoveryListener?.onServiceLost(serviceInfo)
        }
    }

    override fun onDiscoveryStart(resultCode: Int) {
        lanLink.runOnUiThread {
            lanLink.discoveryListener?.onDiscoveryStart(resultCode)
        }
    }
}

class IRegistrationListenerImpl(private val lanLink: LanLink) :
    IRegistrationListener.Stub() {
    override fun onServiceUnregistered(resultCode: Int) {
        lanLink.runOnUiThread {
            lanLink.registrationListener?.onServiceUnregistered(resultCode)
        }
    }

    override fun onServiceRegistered(resultCode: Int) {
        lanLink.runOnUiThread {
            lanLink.registrationListener?.onServiceRegistered(resultCode)
        }
    }
}

class IConnectionListenerImpl(private val lanLink: LanLink) : IConnectionListener.Stub() {
    override fun onConnect(serviceInfo: ServiceInfo, resultCode: Int) {
        lanLink.runOnUiThread {
            lanLink.connectionListener?.onConnect(serviceInfo, resultCode)
        }
    }

    override fun onMessage(serviceInfo: ServiceInfo, msg: Msg?) {
        msg?.let {
            println("onMessage: ${msg.tag}")
            println("onMessage: ${msg.data}")
            val codec = lanLink.messageCodecs[msg.tag]
            if (codec != null && lanLink.messageListener != null) {
                val data = codec.decodeInner(msg)
                lanLink.runOnUiThread {
                    lanLink.messageListener?.onMessage(serviceInfo, msg.tag, data)
                }
            }
        }
    }

    override fun onDisconnect(serviceInfo: ServiceInfo, resultCode: Int) {
        lanLink.runOnUiThread {
            lanLink.connectionListener?.onDisconnect(serviceInfo, resultCode)
        }
    }
}
