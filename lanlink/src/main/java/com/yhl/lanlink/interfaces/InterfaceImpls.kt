package com.yhl.lanlink.interfaces

import com.yhl.lanlink.IConnectionListener
import com.yhl.lanlink.IDiscoveryListener
import com.yhl.lanlink.IRegistrationListener
import com.yhl.lanlink.ServiceInfo

class IDiscoveryListenerImpl(private val listener: DiscoveryListener?) : IDiscoveryListener.Stub() {
    override fun onServiceFound(serviceInfo: ServiceInfo) {
        listener?.onServiceFound(serviceInfo)
    }

    override fun onDiscoveryStop(resultCode: Int) {
        listener?.onDiscoveryStop(resultCode)
    }

    override fun onServiceLost(serviceInfo: ServiceInfo) {
        listener?.onServiceLost(serviceInfo)
    }

    override fun onDiscoveryStart(resultCode: Int) {
        listener?.onDiscoveryStart(resultCode)
    }
}

class IRegistrationListenerImpl(private val listener: RegistrationListener?) :
    IRegistrationListener.Stub() {
    override fun onServiceUnregistered(resultCode: Int) {
        listener?.onServiceUnregistered(resultCode)
    }

    override fun onServiceRegistered(resultCode: Int) {
        listener?.onServiceRegistered(resultCode)
    }
}

class IConnectionListenerImpl(private val listener: ConnectionListener?) : IConnectionListener.Stub() {
    override fun onConnect(serviceInfo: ServiceInfo, resultCode: Int) {
        listener?.onConnect(serviceInfo, resultCode)
    }

    override fun onDisconnect(serviceInfo: ServiceInfo, resultCode: Int) {
        listener?.onDisconnect(serviceInfo, resultCode)
    }
}
