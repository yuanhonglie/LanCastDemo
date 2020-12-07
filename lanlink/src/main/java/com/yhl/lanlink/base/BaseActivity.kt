package com.yhl.lanlink.base

import android.os.Message
import androidx.appcompat.app.AppCompatActivity
import com.yhl.lanlink.FILE_SERVER_PORT
import com.yhl.lanlink.LanLink
import com.yhl.lanlink.MessageListener
import com.yhl.lanlink.util.getIPv4Address

abstract class BaseActivity: AppCompatActivity(), MessageListener {

    override fun onResume() {
        super.onResume()
        LanLink.getInstance().messageListener = this
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onMessage(msg: Message) {
        println("onMessage msg = " + msg.what)
    }

    fun getFileHost() = getIPv4Address()

    protected fun getFileServerUrl() = "http://${getFileHost()}:$FILE_SERVER_PORT"

    protected fun getImageUrl() = "${getFileServerUrl()}/sdcard/media/image1.jpg"
    protected fun getVideoUrl() = "${getFileServerUrl()}/sdcard/media/video03.mp4"
}