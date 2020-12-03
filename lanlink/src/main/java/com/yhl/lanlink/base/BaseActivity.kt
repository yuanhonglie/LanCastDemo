package com.yhl.lanlink.base

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import com.yhl.lanlink.FILE_SERVER_PORT
import com.yhl.lanlink.MESSAGE_SERVER_PORT
import com.yhl.lanlink.MSG_UI_ACTIVITY_REGISTER
import com.yhl.lanlink.data.ActionType
import com.yhl.lanlink.data.Media
import com.yhl.lanlink.data.MediaType
import com.yhl.lanlink.data.TaskInfo
import com.yhl.lanlink.http.HttpClient
import com.yhl.lanlink.http.MediaServerApi
import com.yhl.lanlink.server.HttpService
import com.yhl.lanlink.util.getIPv4Address
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

abstract class BaseActivity: AppCompatActivity() {

    private var serviceMessenger: Messenger? = null
    protected var serviceCache: MutableMap<Class<*>, Any> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        bindService()
    }

    override fun onStop() {
        super.onStop()
        unbindService()
    }

    override fun onDestroy() {
        unbindService()
        super.onDestroy()
    }

    fun startService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, HttpService::class.java))
        } else {
            startService(Intent(this, HttpService::class.java))
        }
    }

    fun stopService() {
        stopService(Intent(this, HttpService::class.java))
    }

    /**
     * 绑定服务
     */
    fun bindService() {
        try {
            val intent = Intent(this, HttpService::class.java)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 解绑service
     */
    open fun unbindService() {
        unbindService(connection)
    }


    private val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            onMessage(msg)
        }
    }

    open protected fun onMessage(msg: Message) {
        println("onMessage msg = " + msg.what)
    }

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            serviceMessenger = Messenger(service)
            sendMessage(MSG_UI_ACTIVITY_REGISTER)
            handler.sendEmptyMessage(MSG_UI_ACTIVITY_REGISTER)
            onConnected()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            onDisconnected()
        }
    }
    private val clientMessenger = Messenger(handler)

    open fun sendMessage(command: Int) {
        sendMessage(command, null)
    }

    @Synchronized
    open fun sendMessage(command: Int, bundle: Bundle?) {
        val message = Message.obtain()
        message.what = command
        if (command == MSG_UI_ACTIVITY_REGISTER) {
            message.replyTo = clientMessenger
        }
        if (bundle != null) message.data = bundle
        try {
            serviceMessenger?.send(message)
        } catch (e: RemoteException) {
            println("sendMessage: error = ${e.message}")
        }
    }

    protected fun onConnected() { }
    protected fun onDisconnected() { }


    abstract fun getBaseUrl(): String

    abstract fun getClientHost(): String

    fun getFileHost() = getIPv4Address()

    protected fun getMessageServerUrl() = "http://${getClientHost()}:$MESSAGE_SERVER_PORT"

    protected fun getFileServerUrl() = "http://${getFileHost()}:$FILE_SERVER_PORT"

    protected fun getImageUrl() = "${getFileServerUrl()}/sdcard/media/image1.jpg"
    protected fun getVideoUrl() = "${getFileServerUrl()}/sdcard/media/video03.mp4"

    protected fun getNetImageUrl() = "http://news.cri.cn/gb/mmsource/images/2013/06/23/2/2211679758122940818.jpg"
    protected fun getNetVideoUrl() = "http://v.mifile.cn/b2c-mimall-media/ed921294fb62caf889d40502f5b38147.mp4"
}