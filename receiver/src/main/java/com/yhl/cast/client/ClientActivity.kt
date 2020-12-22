package com.yhl.cast.client

import android.os.Bundle
import android.util.Log
import android.view.View
import com.bumptech.glide.Glide
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.runtime.Permission
import com.yhl.lanlink.LINK_SERVICE_RECEIVER
import com.yhl.lanlink.LanLinkReceiver
import com.yhl.lanlink.RESULT_SUCCESS
import com.yhl.lanlink.ServiceInfo
import com.yhl.lanlink.base.BaseActivity
import com.yhl.lanlink.data.ActionType
import com.yhl.lanlink.data.Media
import com.yhl.lanlink.data.MediaType
import com.yhl.lanlink.data.TaskInfo
import com.yhl.lanlink.interfaces.RegistrationListener
import com.yhl.lanlink.util.getIPv4Address
import kotlinx.android.synthetic.main.activity_client.*


class ClientActivity : BaseActivity(), RegistrationListener {
    private val TAG = ClientActivity::class.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)

        tvIp.text = getIPv4Address()

        publish.setOnClickListener {
            startServiceBroadcast()
        }

        unpublish.setOnClickListener {
            stopServiceBroadcast()
        }

        btnRequestMessage.setOnClickListener {
            requestMessageServer()
        }

        AndPermission.with(this)
            .runtime()
            .permission(Permission.Group.STORAGE)
            .onGranted { Log.i(TAG, "RequestStorage: onGranted") }
            .onDenied { Log.i(TAG, "RequestStorage: onDenied") }
            .start()

    }

    private fun startServiceBroadcast() {
        val server = LanLinkReceiver.getInstance()
        server.setRegistrationListener(this)
        server.registerService("${LINK_SERVICE_RECEIVER}#1")
    }

    private fun stopServiceBroadcast() {
        LanLinkReceiver.getInstance().unregisterService()
    }

    override fun finish() {
        super.finish()
        stopVideo()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopServiceBroadcast()
    }

    private fun stopVideo() {
        if (vvVideo.isPlaying) {
            vvVideo.stopPlayback()
        }
    }

    private fun playMedia(media: Media) {
        tvContent.text = "playMedia: $media"
        if (media.mediaType == MediaType.video) {
            stopVideo()
            vvVideo.visibility = View.VISIBLE
            ivImage.visibility = View.GONE
            vvVideo.setVideoPath(media.uri)
            //vvVideo.setOnCompletionListener { vvVideo.start() }
            vvVideo.start()
        } else {
            ivImage.visibility = View.VISIBLE
            vvVideo.visibility = View.GONE
            Glide.with(this).load(media.uri).into(ivImage)
        }
    }

    private fun requestMessageServer() {

    }

    override fun onReceive(serviceInfo: ServiceInfo, type: String, data: Any, resultCode: Int) {
        super.onReceive(serviceInfo, type, data, resultCode)
        if (resultCode != RESULT_SUCCESS) {
            println("onMessage: resultCode=$resultCode")
            return
        }
        when (type) {
            TaskInfo::class.qualifiedName -> {
                if (data is TaskInfo) {
                    if (data.actionType == ActionType.cast) {
                        playMedia(data.media)
                    }
                }
            }
        }
    }


    override fun onServiceRegistered(resultCode: Int) {
        println("onServiceRegistered: ${resultCode}")
    }

    override fun onServiceUnregistered(resultCode: Int) {
        println("onServiceUnregistered: ${resultCode}")
    }
}