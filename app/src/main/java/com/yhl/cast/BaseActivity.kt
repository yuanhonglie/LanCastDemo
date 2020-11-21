package com.yhl.cast

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import com.yhl.data.ActionType
import com.yhl.data.Media
import com.yhl.data.MediaType
import com.yhl.data.TaskInfo
import com.yhl.http.HttpClient
import com.yhl.http.MediaServerApi
import com.yhl.server.FILE_SERVER_PORT
import com.yhl.server.HttpService
import com.yhl.server.MESSAGE_SERVER_PORT
import com.yhl.server.MSG_ACTIVITY_REGISTER
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

abstract class BaseActivity: AppCompatActivity() {

    private var serviceMessenger: Messenger? = null
    protected var serverApi: MediaServerApi? = null
    protected lateinit var retrofit: Retrofit
    protected var serviceCache: MutableMap<Class<*>, Any> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retrofit = Retrofit.Builder()
            .baseUrl(getBaseUrl())
            .client(HttpClient.client)
            .addConverterFactory(getConverterFactory())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
        serverApi = getService(MediaServerApi::class.java)
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
            sendMessage(MSG_ACTIVITY_REGISTER)
            handler.sendEmptyMessage(MSG_ACTIVITY_REGISTER)
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
        if (command == MSG_ACTIVITY_REGISTER) {
            message.replyTo = clientMessenger
        }
        if (bundle != null) message.data = bundle
        try {
            serviceMessenger?.send(message)
        } catch (e: RemoteException) {
            e.printStackTrace()
            println("sendMessage: error = ${e.message}")
        }
    }

    protected fun onConnected() { }
    protected fun onDisconnected() { }


    abstract fun getBaseUrl(): String

    protected open fun getConverterFactory(): Converter.Factory? {
        val gson = GsonBuilder().setLenient().create()
        return GsonConverterFactory.create(gson)
    }

    @Synchronized
    protected fun getService(service: Class<MediaServerApi>): MediaServerApi? {
        if (serviceCache.containsKey(service)) {
            return serviceCache.get(service) as MediaServerApi
        }
        val api = retrofit.create<MediaServerApi>(service)
        serviceCache[service] = api
        return api
    }



    abstract fun getHost(): String

    protected fun getMessageServerUrl() = "http://${getHost()}:$MESSAGE_SERVER_PORT"

    protected fun getFileServerUrl() = "http://${getHost()}:$FILE_SERVER_PORT"

    protected fun getImageUrl() = "${getFileServerUrl()}/sdcard/media/image1.jpg"
    protected fun getVideoUrl() = "${getFileServerUrl()}/sdcard/media/video03.mp4"

    protected fun getNetImageUrl() = "http://news.cri.cn/gb/mmsource/images/2013/06/23/2/2211679758122940818.jpg"
    protected fun getNetVideoUrl() = "http://v.mifile.cn/b2c-mimall-media/ed921294fb62caf889d40502f5b38147.mp4"

    protected fun sendCastTask(uri: String, type: MediaType) {
        val media = Media(uri,type)
        val taskInfo = TaskInfo(media, ActionType.cast)

        serverApi?.let {
            it.requestTransfer(taskInfo)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    println(it)
                }
        }
    }
}