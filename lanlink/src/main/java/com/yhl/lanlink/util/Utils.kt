package com.yhl.lanlink.util

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_SERVICES
import android.os.Process
import android.text.TextUtils
import com.yhl.lanlink.log.Logger
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.security.MessageDigest
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

fun isInServiceProcess(
    context: Context,
    serviceClass: Class<out Service>
): Boolean {
    val packageManager = context.packageManager
    val packageInfo = try {
        packageManager.getPackageInfo(context.getPackageName(), GET_SERVICES)
    } catch (e: java.lang.Exception) {
        return false
    }

    val mainProcess = packageInfo.applicationInfo.processName
    val component = ComponentName(context, serviceClass)
    val serviceInfo = try {
        packageManager.getServiceInfo(component, PackageManager.MATCH_DISABLED_COMPONENTS)
    } catch (ignored: PackageManager.NameNotFoundException) {
        return false
    }

    if (serviceInfo.processName == mainProcess) {
        return false
    }

    val myPid = Process.myPid()
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    var myProcess: RunningAppProcessInfo? = null

    val runningProcesses = try {
        activityManager.runningAppProcesses
    } catch (exception: SecurityException) {
        return false
    }

    if (runningProcesses != null) {
        for (process in runningProcesses) {
            if (process.pid == myPid) {
                myProcess = process
                break
            }
        }
    }

    return myProcess?.processName == serviceInfo.processName
}

fun getIPv4Address() = getIPAddress(true)

fun getIPv6Address() = getIPAddress(false)

fun String.md5(): String {
    if (TextUtils.isEmpty(this)) {
        return ""
    }

    return try {
        val instance: MessageDigest = MessageDigest.getInstance("MD5")//获取md5加密对象
        val digest: ByteArray = instance.digest(toByteArray())//对字符串加密，返回字节数组
        var sb = StringBuffer()
        for (b in digest) {
            var i: Int = b.toInt() and 0xff //获取低八位有效值
            var hexString = Integer.toHexString(i) //将整数转化为16进制
            if (hexString.length < 2) {
                hexString = "0" + hexString //如果是一位的话，补0
            }
            sb.append(hexString)
        }
        sb.toString()
    } catch (e: Exception) {
        Logger.e("String", "md5: error = " + e.message)
        ""
    }
}