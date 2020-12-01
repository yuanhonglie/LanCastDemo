package com.yhl.cast.client

import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.View
import com.bumptech.glide.Glide
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.runtime.Permission
import com.yhl.lanlink.LINK_SERVICE_RECEIVER
import com.yhl.lanlink.base.BaseActivity
import com.yhl.lanlink.data.Media
import com.yhl.lanlink.data.MediaType
import com.yhl.lanlink.data.TaskInfo
import com.yhl.lanlink.http.HttpClient
import com.yhl.lanlink.nsd.RegistrationListener
import com.yhl.lanlink.nsd.ServiceManager
import com.yhl.lanlink.util.getIPv4Address
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_client.*
import okhttp3.Request


class ClientActivity : BaseActivity(), RegistrationListener {
    private val TAG = ClientActivity::class.simpleName
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)

        tvIp.text = getIPv4Address()

        btnStart.setOnClickListener {
            startServiceBroadcast()
        }

        btnRequestMessage.setOnClickListener {
            requestMessageServer()
        }

        btnRequestImage.setOnClickListener {
            ivImage.visibility = View.VISIBLE
            vvVideo.visibility = View.GONE
            Glide.with(this).load(getImageUrl()).into(ivImage)
        }

        btnRequestVideo.setOnClickListener {
            stopVideo()
            vvVideo.visibility = View.VISIBLE
            ivImage.visibility = View.GONE
            vvVideo.setVideoPath(getVideoUrl())
            vvVideo.setOnCompletionListener { vvVideo.start() }
            vvVideo.start()
        }

        AndPermission.with(this)
            .runtime()
            .permission(Permission.Group.STORAGE)
            .onGranted { Log.i(TAG, "RequestStorage: onGranted") }
            .onDenied { Log.i(TAG, "RequestStorage: onDenied") }
            .start()

    }

    private fun startServiceBroadcast() {
        val serviceManager = ServiceManager.getInstance(this)
        serviceManager.mRegistrationListener = this
        serviceManager.registerService("${LINK_SERVICE_RECEIVER}#1")
    }

    private fun stopServiceBroadcast() {
        ServiceManager.getInstance(this).unregisterService()
    }

    override fun getBaseUrl() = getMessageServerUrl()

    override fun getClientHost() = "192.168.12.110"

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
        Observable.create<String> { emitter: ObservableEmitter<String> ->
            val response = HttpClient.client.newCall(createHttpRequest()).execute()
            response.body?.let {
                emitter.onNext(it.string())
                emitter.onComplete()
            }
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                Log.i(TAG, "requestServer: response = $it")
                tvContent.text = it
            }
    }

    private fun createHttpRequest() = Request.Builder().url(getMessageServerUrl()).build()

    override fun onMessage(msg: Message) {
        super.onMessage(msg)
        when (msg.what) {
            100 -> {
                val taskInfo = msg.obj
                if (taskInfo is TaskInfo) {
                    val media = taskInfo.media
                    playMedia(media)
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