package com.yhl.cast

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.runtime.Permission
import com.yhl.http.HttpClient
import com.yhl.server.FILE_SERVER_PORT
import com.yhl.server.HttpService
import com.yhl.server.MESSAGE_SERVER_PORT
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.Request


class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.simpleName

    val host: String = "127.0.0.1"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //host = getIPv4Address()
        tvIp.text = host

        btnStart.setOnClickListener {
            startService(Intent(this, HttpService::class.java))
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
            vvVideo.visibility = View.VISIBLE
            ivImage.visibility = View.GONE
            vvVideo.setVideoPath(getVideoUrl())
            vvVideo.start()
        }

        AndPermission.with(this)
                .runtime()
                .permission(Permission.Group.STORAGE)
                .onGranted { Log.i(TAG, "RequestStorage: onGranted") }
                .onDenied{ Log.i(TAG, "RequestStorage: onDenied")}
                .start()
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

    private fun getMessageServerUrl() = "http://$host:${MESSAGE_SERVER_PORT}"

    private fun getFileServerUrl() = "http://$host:${FILE_SERVER_PORT}"

    private fun getImageUrl() = "${getFileServerUrl()}/sdcard/download/sky.jpg"
    private fun getVideoUrl() = "${getFileServerUrl()}/sdcard/download/sky.jpg"

    private fun getRemoteImageUrl() = "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1605889829717&di=60255c3716346be17816b81ae1c64a02&imgtype=0&src=http%3A%2F%2Fimg1.gtimg.com%2Fhn%2Fpics%2Fhv1%2F51%2F217%2F2144%2F139468986.jpg"
    private fun getRemoteVideoUrl() = "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1605889829717&di=60255c3716346be17816b81ae1c64a02&imgtype=0&src=http%3A%2F%2Fimg1.gtimg.com%2Fhn%2Fpics%2Fhv1%2F51%2F217%2F2144%2F139468986.jpg"
}