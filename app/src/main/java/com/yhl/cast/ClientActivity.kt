package com.yhl.cast

import android.content.Intent
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.View
import com.bumptech.glide.Glide
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.runtime.Permission
import com.yhl.data.Media
import com.yhl.data.MediaType
import com.yhl.data.TaskInfo
import com.yhl.http.HttpClient
import com.yhl.server.HttpService
import com.yhl.util.getIPv4Address
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.Request


class ClientActivity : BaseActivity() {
    private val TAG = ClientActivity::class.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvIp.text = getIPv4Address()

        btnStart.setOnClickListener {

        }

        btnRequestMessage.setOnClickListener {
            requestMessageServer()
        }

        btnRequestImage.setOnClickListener {
            ivImage.visibility = View.VISIBLE
            vvVideo.visibility = View.GONE
            Glide.with(this).load(getImageUrl()).into(ivImage)
        }

        btnRequestVideo.setOnClickListener{
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
                .onDenied{ Log.i(TAG, "RequestStorage: onDenied")}
                .start()
    }

    override fun getBaseUrl() = getMessageServerUrl()

    override fun getHost() = "192.168.12.110"

    override fun finish() {
        super.finish()
        stopVideo()
    }

    private fun stopVideo() {
        if (vvVideo.isPlaying) {
            vvVideo.stopPlayback()
        }
    }

    private fun playMedia(media: Media) {
        if (media.mediaType == MediaType.video) {
            stopVideo()
            vvVideo.visibility = View.VISIBLE
            ivImage.visibility = View.GONE
            vvVideo.setVideoPath(getVideoUrl())
            //vvVideo.setOnCompletionListener { vvVideo.start() }
            vvVideo.start()
        } else {
            ivImage.visibility = View.VISIBLE
            vvVideo.visibility = View.GONE
            Glide.with(this).load(getImageUrl()).into(ivImage)
        }
    }

    private fun requestMessageServer() {
        Observable.create<String> {emitter: ObservableEmitter<String> ->
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
        when(msg.what) {
            100 -> {
                val taskInfo = msg.obj
                if (taskInfo is TaskInfo) {
                    val media = taskInfo.media
                    playMedia(media)
                }
            }
        }
    }

}