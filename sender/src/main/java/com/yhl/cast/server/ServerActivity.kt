package com.yhl.cast.server

import android.os.Bundle
import android.os.Handler
import com.yhl.lanlink.base.BaseActivity
import com.yhl.lanlink.data.MediaType
import com.yhl.lanlink.data.ServiceInfo
import com.yhl.lanlink.nsd.DiscoveryListener
import com.yhl.lanlink.nsd.ServiceManager
import kotlinx.android.synthetic.main.activity_server.*


class ServerActivity : BaseActivity(), DiscoveryListener {
    private var mServiceInfo: ServiceInfo? = null
    private var mUiHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)

        btnSendImage.setOnClickListener {
            sendCastTask(getFileServerUrl() + getImageUri(), MediaType.image)
        }

        btnSendVideo.setOnClickListener {
            sendCastTask(getFileServerUrl() + getVideoUri(), MediaType.video)
        }

        btnDiscover.setOnClickListener {
            startServiceDiscover()
        }

        btnStopDiscover.setOnClickListener {
            stopServiceDiscover()
        }
    }

    private fun startServiceDiscover() {
        ServiceManager.getInstance(this).mDiscoveryListener = this
        ServiceManager.getInstance(this).startDiscovery()
    }

    private fun stopServiceDiscover() {
        ServiceManager.getInstance(this).stopDiscovery()
        mServiceInfo = null
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun getImageUri() = if (etImagePath.text.isEmpty().not()) etImagePath.text.toString() else "/sdcard/media/image1.jpg"
    private fun getVideoUri() = if (etVideoPath.text.isEmpty().not()) etVideoPath.text.toString() else "/sdcard/media/video03.mp4"

    override fun getBaseUrl() = getMessageServerUrl()
    override fun getClientHost() = mServiceInfo?.host?.hostAddress ?: "xxx.xxx.xxx.xxx"

    override fun onDiscoveryStart(resultCode: Int) {
        println("onDiscoveryStart: ${resultCode}")
    }

    override fun onDiscoveryStop(resultCode: Int) {
        println("onDiscoveryStop: ${resultCode}")
    }

    override fun onServiceFound(serviceInfo: ServiceInfo) {
        println("onServiceFound: ${serviceInfo}")
        mServiceInfo = serviceInfo
        mUiHandler.post {
            tvDevice.setText("Service found:\n$mServiceInfo")
        }
    }

    override fun onServiceLost(serviceInfo: ServiceInfo) {
        println("onServiceLost: ${serviceInfo}")
    }
}
