package com.yhl.lanlink.base

import androidx.appcompat.app.AppCompatActivity
import com.yhl.lanlink.FILE_SERVER_PORT
import com.yhl.lanlink.LanLink
import com.yhl.lanlink.ServiceInfo
import com.yhl.lanlink.interfaces.MessageListener
import com.yhl.lanlink.util.getIPv4Address

abstract class BaseActivity() : AppCompatActivity(), MessageListener {

    override fun onResume() {
        super.onResume()
        LanLink.getInstance().messageListener = this
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onMessage(serviceInfo: ServiceInfo, type: String, data: Any) {

    }

    fun getFileHost() = getIPv4Address()

    protected fun getFileServerUrl() = "http://${getFileHost()}:$FILE_SERVER_PORT"

    protected fun getImageUrl() = "${getFileServerUrl()}/sdcard/media/image1.jpg"
    protected fun getVideoUrl() = "${getFileServerUrl()}/sdcard/media/video03.mp4"
}