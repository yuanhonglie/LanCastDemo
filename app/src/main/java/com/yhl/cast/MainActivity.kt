package com.yhl.cast

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.yhl.http.HttpClient
import com.yhl.server.FILE_SERVER_PORT
import com.yhl.server.HttpService
import com.yhl.server.MESSAGE_SERVER_PORT
import com.yhl.util.getIPv4Address
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.Request



class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.simpleName
    val host: String = "localhost"
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

        btnRequestFile.setOnClickListener {
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

    private fun getMessageServerUrl() = "http://$host:${MESSAGE_SERVER_PORT}"

    private fun getFileServerUrl() = "http://$host:${FILE_SERVER_PORT}"

    private fun getImageUrl() = "${getFileServerUrl()}/sdcard/galaxy.jpg"
}