package com.yhl.lanlink.util

import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*


private fun getIPAddress(useIPv4: Boolean): String {
    try {
        val nis = NetworkInterface.getNetworkInterfaces()
        val adds = LinkedList<InetAddress>()
        while (nis.hasMoreElements()) {
            val ni = nis.nextElement()
            // 防止小米手机返回 10.0.2.15
            if (!ni.isUp || ni.isLoopback) {
                continue
            }
            val addresses = ni.inetAddresses
            while (addresses.hasMoreElements()) {
                adds.addFirst(addresses.nextElement())
            }
        }
        for (add in adds) {
            if (!add.isLoopbackAddress) {
                val hostAddress = add.hostAddress
                val isIPv4 = hostAddress.indexOf(':') < 0
                if (useIPv4) {
                    if (isIPv4) {
                        return hostAddress
                    }
                } else {
                    if (!isIPv4) {
                        val index = hostAddress.indexOf('%')
                        return if (index < 0) hostAddress.toUpperCase() else hostAddress.substring(
                            0,
                            index
                        ).toUpperCase()
                    }
                }
            }
        }
    } catch (e: SocketException) {
        e.printStackTrace()
    }
    return ""
}

fun getIPv4Address() = getIPAddress(true)

fun getIPv6Address() = getIPAddress(false)