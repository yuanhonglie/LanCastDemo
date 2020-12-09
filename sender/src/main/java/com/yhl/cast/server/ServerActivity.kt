package com.yhl.cast.server

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.widget.Toast
import com.yhl.lanlink.*
import com.yhl.lanlink.base.BaseActivity
import com.yhl.lanlink.data.ActionType
import com.yhl.lanlink.data.MediaType
import com.yhl.lanlink.interfaces.ConnectionListener
import com.yhl.lanlink.interfaces.DiscoveryListener
import kotlinx.android.synthetic.main.activity_server.*


class ServerActivity : BaseActivity(), DiscoveryListener {
    private var mServiceInfo: ServiceInfo? = null
    private var mUiHandler = Handler()
    private lateinit var mLanLink: LanLink

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
        mLanLink = LanLink.getInstance()
        mLanLink.connectionListener = object : ConnectionListener {
            override fun onConnect(serviceInfo: ServiceInfo, resultCode: Int) {
                println("onConnect: $serviceInfo resultCode = $resultCode")
                if (resultCode == RESULT_SUCCESS) {
                    toast("连接客户端${serviceInfo.name}成功")
                } else {
                    toast("连接客户端${serviceInfo.name}失败")
                }
            }

            override fun onDisconnect(serviceInfo: ServiceInfo, resultCode: Int) {
                println("onDisconnect: $serviceInfo resultCode = $resultCode")
                toast("断开接收端${serviceInfo.name}, $resultCode")
            }
        }
    }

    private fun sendCastTask(uri: String, type: MediaType) {
        val serviceInfo = mServiceInfo
        if (serviceInfo != null) {
            println("sendCastTask isConnected = ${serviceInfo.isConnected()}")
            LanLink.getInstance().sendCastTask(serviceInfo, uri, type, ActionType.cast)
        } else {
            toast("请先连接接收端！")
        }
    }

    private fun connectService(serviceInfo: ServiceInfo?) {
        if (serviceInfo != null) {
            mLanLink.connect(serviceInfo)
        }
    }

    private fun disconnectService(serviceInfo: ServiceInfo?) {
        if (serviceInfo != null) {
            mLanLink.disconnect(serviceInfo)
        }
    }

    private fun startServiceDiscover() {
        LanLink.getInstance().discoveryListener = this
        LanLink.getInstance().startDiscovery()
    }

    private fun stopServiceDiscover() {
        LanLink.getInstance().stopDiscovery()
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

    fun Context.toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
