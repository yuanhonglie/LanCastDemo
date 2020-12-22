package com.yhl.lanlink.interfaces

import com.yhl.lanlink.*
import com.yhl.lanlink.log.Logger

private const val TAG = "interfaces"
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

    override fun onMessageReceive(serviceInfo: ServiceInfo, msg: Msg?) {
        msg?.let {
            Logger.i(TAG, "onMessage: ${msg.tag}")
            val codec = lanLink.getMessageCodec(msg.tag)
            val messageListener = lanLink.getMessageListener()
            if (messageListener != null) {
                val resultCode = RESULT_SUCCESS
                if (codec == null) RESULT_FAILED_MESSAGE_PARSER_MISSING else RESULT_SUCCESS
                val data = codec?.decodeInner(msg) ?: msg.data
                lanLink.runOnUiThread {
                    messageListener.onReceive(serviceInfo, msg.tag, data, resultCode)
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
