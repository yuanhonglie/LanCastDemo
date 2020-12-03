package com.yhl.cast.server

import android.os.Bundle
import android.os.Handler
import com.yhl.lanlink.RESULT_SUCCESS
import com.yhl.lanlink.base.BaseActivity
import com.yhl.lanlink.channel.Channel
import com.yhl.lanlink.data.MediaType
import com.yhl.lanlink.data.ServiceInfo
import com.yhl.lanlink.nsd.ConnectionListener
import com.yhl.lanlink.nsd.DiscoveryListener
import com.yhl.lanlink.nsd.ServiceManager
import kotlinx.android.synthetic.main.activity_server.*


class ServerActivity : BaseActivity(), DiscoveryListener {
    private var mServiceInfo: ServiceInfo? = null
    private var mChannel: Channel? = null
    private var mUiHandler = Handler()
    private lateinit var mServiceManager: ServiceManager

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

        btnConnect.setOnClickListener {
            val serviceInfo = mServiceInfo
            connectService(serviceInfo)
        }

        btnDisconnect.setOnClickListener {
            val serviceInfo = mServiceInfo
            disconnectService(serviceInfo)
        }
        mServiceManager = ServiceManager.getInstance(this)
        mServiceManager.mConnectionListener = object : ConnectionListener {
            override fun onConnect(serviceInfo: ServiceInfo, resultCode: Int) {
                println("onConnect: $serviceInfo resultCode = $resultCode")
                if (resultCode == RESULT_SUCCESS) {
                    mChannel = serviceInfo.channel
                }
            }

            override fun onDisconnect(serviceInfo: ServiceInfo, resultCode: Int) {
                println("onDisconnect: $serviceInfo resultCode = $resultCode")
                mChannel = null
            }

        }
    }

    private fun sendCastTask(uri: String, type: MediaType) {
        mChannel?.sendCastTask(uri, type)
    }

    private fun connectService(serviceInfo: ServiceInfo?) {
        if (serviceInfo != null) {
            mServiceManager.connect(serviceInfo)
        }
    }

    private fun disconnectService(serviceInfo: ServiceInfo?) {
        if (serviceInfo != null) {
            mServiceManager.disconnect(serviceInfo)
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

    private fun getImageUri() = if (etImagePath.text.isEmpty()
            .not()
    ) etImagePath.text.toString() else "/sdcard/media/image1.jpg"

    private fun getVideoUri() = if (etVideoPath.text.isEmpty()
            .not()
    ) etVideoPath.text.toString() else "/sdcard/media/video03.mp4"

    override fun getBaseUrl() = getMessageServerUrl()
    override fun getClientHost() = mServiceInfo?.host?.hostAddress ?: "xxx.xxx.xxx.xxx"

    override fun onDiscoveryStart(resultCode: Int) {
        println("onDiscoveryStart: ${resultCode}")
    }

    override fun onDiscoveryStop(resultCode: Int) {
        println("onDiscoveryStop: ${resultCode}")
    }

    override fun onServiceFound(serviceInfo: ServiceInfo) {
        println("onServiceFound: ${serviceInfo} id=${serviceInfo.id}")
        mServiceInfo = serviceInfo
        mUiHandler.post {
            tvDevice.setText("Service found:\n$mServiceInfo id=${serviceInfo.id}")
        }
    }

    override fun onServiceLost(serviceInfo: ServiceInfo) {
        println("onServiceLost: ${serviceInfo} id=${serviceInfo.id}")
    }
}
