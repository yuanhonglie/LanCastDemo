package com.yhl.lanlink.server

import android.os.Messenger
import com.yhl.lanlink.*
import com.yhl.lanlink.data.MediaType
import com.yhl.lanlink.nsd.ServiceManager

class ServiceBinder(private val serviceManager: ServiceManager) : ILanLinkService.Stub() {
    override fun stopDiscovery() {
        serviceManager.stopDiscovery()
    }

    override fun setClientMessenger(messenger: Messenger?) {
        serviceManager.setClientMessenger(messenger)
    }

    override fun connect(serviceId: String) {
        serviceManager.connect(serviceId)
    }

    override fun setRegistrationListener(listener: IRegistrationListener) {
        serviceManager.setRegistrationListener(listener)
    }

    override fun setConnectionListener(listener: IConnectionListener?) {
        serviceManager.setConnectionListener(listener)
    }

    override fun startDiscovery() {
        serviceManager.startDiscovery()
    }

    override fun unregisterService() {
        serviceManager.unregisterService()
    }

    override fun registerService(name: String) {
        serviceManager.registerService(name)
    }

    override fun destroy() {
        serviceManager.destroy()
    }

    override fun disconnect(serviceId: String) {
        serviceManager.disconnect(serviceId)
    }

    override fun setDiscoveryListener(listener: IDiscoveryListener?) {
        serviceManager.setDiscoveryListener(listener)
    }

    override fun sendCastTask(serviceId: String?, uri: String?, mediaType: String?) {
        serviceManager.sendCastTask(serviceId, uri, mediaType)
    }
}