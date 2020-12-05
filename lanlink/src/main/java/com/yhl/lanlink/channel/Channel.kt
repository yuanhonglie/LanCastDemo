package com.yhl.lanlink.channel

import androidx.annotation.WorkerThread
import com.google.gson.GsonBuilder
import com.yhl.lanlink.*
import com.yhl.lanlink.data.ActionType
import com.yhl.lanlink.data.Media
import com.yhl.lanlink.data.MediaType
import com.yhl.lanlink.data.TaskInfo
import com.yhl.lanlink.http.HttpClient
import com.yhl.lanlink.http.MediaServerApi
import com.yhl.lanlink.nsd.ServiceManager
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

internal class Channel(private val mWorkerHandler: ServiceManager.WorkerHandler, internal val server: ServiceInfo) {

    private var mToken: String? = null
    private var mLost = 0
    private var mApi: MediaServerApi
    @Volatile
    var isActive = false
    private set(value) {
        field = value
    }

    @Volatile
    var isConnected = false
    private set(value) {
        field = value
    }

    init {
        val baseUrl = "http://${server.host}:${server.port}"
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(HttpClient.client)
            .addConverterFactory(getConverterFactory())
            .build()
        mApi = retrofit.create<MediaServerApi>(MediaServerApi::class.java)
    }

    private fun getConverterFactory(): Converter.Factory {
        val gson = GsonBuilder().setLenient().create()
        return GsonConverterFactory.create(gson)
    }

    /**
     * 连接server
     */
    fun connect() {
        runOnWorkerThread {
            requestConnect()
        }
    }

    /**
     * 断开server
     */
    fun disconnect() {
        runOnWorkerThread {
            requestDisconnect()
        }
    }

    @WorkerThread
    fun heartbeat() {
        if (isActive) {
            if (requestHeartbeat()) {
                mLost = 0
            } else {
                mLost++
            }
            println("heartBeat: $mLost")
            if (mLost >= MAX_SERVER_HEART_BEAT_LOST) {
                onDisconnect(RESULT_FAILED_SERVER_TIMEOUT)
                return
            }
            scheduleHeartbeat()
        }
    }

    fun sendCastTask(uri: String, type: MediaType) {
        runOnWorkerThread {
            requestCastTask(uri, type)
        }
    }

    private fun onConnect(resultCode: Int) {
        isActive = true
        val msg = mWorkerHandler.obtainMessage(MSG_WORKER_SERVER_CONNECT)
        msg.obj = this
        msg.arg1 = resultCode
        msg.sendToTarget()
        if (resultCode == RESULT_SUCCESS) {
            isConnected = true
            server.channel = this
            scheduleHeartbeat()
        }
    }

    private fun onDisconnect(resultCode: Int) {
        close()
        val msg = mWorkerHandler.obtainMessage(MSG_WORKER_SERVER_DISCONNECT)
        msg.obj = this
        msg.arg1 = resultCode
        msg.sendToTarget()
    }

    private fun scheduleHeartbeat() {
        val msg = mWorkerHandler.obtainMessage(MSG_WORKER_HEART_BEAT)
        msg.obj = this
        mWorkerHandler.sendMessageDelayed(msg, INTERVAL_HEART_BEAT)
    }

    @WorkerThread
    private fun requestConnect() {
        try {
            val call = mApi.requestConnection(server)
            val response = call.execute()
            val result = response.body()
            if (result != null && result.errorCode == RESULT_SUCCESS) {
                mToken = result.data
                println("connect: token = $mToken")
                onConnect(if (mToken == null) RESULT_FAILED_INVALID_TOKEN else RESULT_SUCCESS)
            } else {
                onConnect(RESULT_FAILED)
            }
        } catch (e: Exception) {
            println("connect: error = ${e.message}")
            onConnect(RESULT_FAILED)
        }
    }

    @WorkerThread
    private fun requestDisconnect() {
        try {
            val token = mToken
            if (token != null) {
                val call = mApi.requestDisconnection(token)
                val response = call.execute()
                val result = response.body()
                if (result != null && result.errorCode == RESULT_SUCCESS) {
                    onDisconnect(RESULT_SUCCESS)
                } else {
                    onDisconnect(RESULT_FAILED)
                }
            } else {
                onDisconnect(RESULT_FAILED_INVALID_TOKEN)
            }
        } catch (e: Exception) {
            println("disconnect: error = ${e.message}")
            onDisconnect(RESULT_FAILED)
        }
    }

    @WorkerThread
    private fun requestHeartbeat(): Boolean {
        return try {
            val token = mToken
            return if (token != null) {
                val call = mApi.requestHeartBeat(token, System.currentTimeMillis())
                val response = call.execute()
                val result = response.body()
                if (result != null && result.errorCode == RESULT_SUCCESS) {
                    val requestTimestamp = result.data as Long
                    val nowTime = System.currentTimeMillis()
                    val timeCost = nowTime - requestTimestamp
                    println("requestHeartbeat: timeCost = $timeCost")
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            println("requestHeartbeat: error = ${e.message}")
            false
        }
    }

    @WorkerThread
    private fun requestCastTask(uri: String, type: MediaType) {
        val media = Media(uri,type)
        val taskInfo = TaskInfo(media, ActionType.cast)

        try {
            val token = mToken
            if (token != null) {
                val call = mApi.requestTransfer(token, taskInfo)
                val response = call.execute()
                val result = response.body()
                println("requestCastTask: result = $result")
                if (result != null && result.errorCode == RESULT_SUCCESS) {
                }
            } else {
                println("requestCastTask: invalid token")
            }
        } catch (e: Exception) {
            println("requestCastTask: error = ${e.message}")
        }

    }

    private fun runOnWorkerThread(r: () -> Unit) {
        mWorkerHandler.post(r)
    }

    private fun close() {
        isConnected = false
        isActive = false
        mToken = null
        server.channel = null
    }
}