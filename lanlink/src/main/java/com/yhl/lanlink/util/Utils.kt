package com.yhl.lanlink.util

import android.text.TextUtils
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.security.MessageDigest
import java.util.*
import kotlin.experimental.and


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

fun String.md5(): String {
    if (TextUtils.isEmpty(this)) {
        return ""
    }

    return try {
        val instance:MessageDigest = MessageDigest.getInstance("MD5")//获取md5加密对象
        val digest:ByteArray = instance.digest(toByteArray())//对字符串加密，返回字节数组
        var sb = StringBuffer()
        for (b in digest) {
            var i :Int = b.toInt() and 0xff //获取低八位有效值
            var hexString = Integer.toHexString(i) //将整数转化为16进制
            if (hexString.length < 2) {
                hexString = "0" + hexString //如果是一位的话，补0
            }
            sb.append(hexString)
        }
        sb.toString()
    } catch (e: Exception) {
        println("md5: error = " + e.message)
        ""
    }
}