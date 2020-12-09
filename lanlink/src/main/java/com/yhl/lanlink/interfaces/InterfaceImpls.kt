package com.yhl.lanlink.interfaces

import com.yhl.lanlink.*

class IDiscoveryListenerImpl(private val lanLink: LanLink) : IDiscoveryListener.Stub() {
    override fun onServiceFound(serviceInfo: ServiceInfo) {
        lanLink.discoveryListener?.onServiceFound(serviceInfo)
    }

    override fun onDiscoveryStop(resultCode: Int) {
        lanLink.discoveryListener?.onDiscoveryStop(resultCode)
    }

    override fun onServiceLost(serviceInfo: ServiceInfo) {
        lanLink.discoveryListener?.onServiceLost(serviceInfo)
    }

    override fun onDiscoveryStart(resultCode: Int) {
        lanLink.discoveryListener?.onDiscoveryStart(resultCode)
    }
}

class IRegistrationListenerImpl(private val lanLink: LanLink) :
    IRegistrationListener.Stub() {
    override fun onServiceUnregistered(resultCode: Int) {
        lanLink.registrationListener?.onServiceUnregistered(resultCode)
    }

    override fun onServiceRegistered(resultCode: Int) {
        lanLink.registrationListener?.onServiceRegistered(resultCode)
    }
}

class IConnectionListenerImpl(private val lanLink: LanLink) : IConnectionListener.Stub() {
    override fun onConnect(serviceInfo: ServiceInfo, resultCode: Int) {
        lanLink.connectionListener?.onConnect(serviceInfo, resultCode)
    }

    override fun onMessage(serviceInfo: ServiceInfo, msg: Msg?) {
        msg?.let {
            println("onMessage: ${msg.tag}")
            println("onMessage: ${msg.data}")
            val codec = lanLink.messageCodecs[msg.tag]
            if (codec != null) {
                val data = codec.decodeInner(msg)
                lanLink.connectionListener?.onMessage(serviceInfo, msg.tag, data)
            }
        }
    }

    override fun onDisconnect(serviceInfo: ServiceInfo, resultCode: Int) {
        lanLink.connectionListener?.onDisconnect(serviceInfo, resultCode)
    }
}
