package com.yhl.lanlink.channel

import androidx.annotation.WorkerThread
import com.google.gson.GsonBuilder
import com.yhl.lanlink.*
import com.yhl.lanlink.data.ActionType
import com.yhl.lanlink.data.Media
import com.yhl.lanlink.data.MediaType
import com.yhl.lanlink.data.TaskInfo
import com.yhl.lanlink.http.HttpClient
import com.yhl.lanlink.http.MessageServerApi
import com.yhl.lanlink.log.Logger
import com.yhl.lanlink.server.ConnectionManager
import com.yhl.lanlink.server.ServiceManager
import com.yhl.lanlink.util.getIPv4Address
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

internal class Channel(
    private val mLocalConnection: ConnectionManager,
    private val mWorkerHandler: ServiceManager.WorkerHandler,
    internal val mServer: ServiceInfo,
    private var mToken: String? = null
) {
    private val TAG = "Channel"
    private var mLost = 0
    private var mApi: MessageServerApi
    @Volatile
    var isActive = false
    private set

    @Volatile
    var isConnected = false
    private set

    init {
        val baseUrl = "http://${mServer.host}:${mServer.port}"
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(HttpClient.client)
            .addConverterFactory(getConverterFactory())
            .build()
        mApi = retrofit.create<MessageServerApi>(MessageServerApi::class.java)
        isConnected = mToken.isNullOrEmpty().not()
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
            Logger.i(TAG, "heartBeat: $mLost")
            if (mLost >= MAX_SERVER_HEART_BEAT_LOST) {
                onDisconnect(RESULT_FAILED_RECEIVER_TIMEOUT)
                return
            }
            scheduleHeartbeat()
        }
    }

    fun sendMessage(msg: Msg) {
        runOnWorkerThread {
            requestSendMessage(msg)
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
            mServer.channel = this
            scheduleHeartbeat()
        }
    }

    private fun onDisconnect(resultCode: Int) {
        mLocalConnection.unregisterClient(mServer)
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
            //连接远端服务成功，将远端结点注册到本地服务，后续远端结点也可以发送消息给本地服务
            val localToken = mLocalConnection.registerClient(mServer)
            //将本地的结点信息传递给远端服务
            val client = ClientInfo("锤子手机", getIPv4Address(), MESSAGE_SERVER_PORT, localToken)
            val call = mApi.requestConnection(client)
            val response = call.execute()
            val result = response.body()
            if (result != null && result.errorCode == RESULT_SUCCESS) {
                mToken = result.data
                Logger.i(TAG, "connect: token = $mToken")
                onConnect(if (mToken == null) RESULT_FAILED_INVALID_TOKEN else RESULT_SUCCESS)
            } else {
                onConnect(RESULT_FAILED)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "connect: error = ${e.message}")
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
            Logger.e(TAG, "disconnect: error = ${e.message}")
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
                    Logger.i(TAG, "requestHeartbeat: timeCost = $timeCost")
                    true
                } else {
                    false
                }
            } else {
                Logger.e(TAG, "requestHeartbeat: invalid token")
                false
            }
        } catch (e: Exception) {
            Logger.e(TAG, "requestHeartbeat: error = ${e.message}")
            false
        }
    }

    @WorkerThread
    private fun requestCastTask(uri: String, type: MediaType, action: ActionType) {
        val media = Media(uri, type)
        media.name = File(uri).name
        val taskInfo = TaskInfo(media, action)

        try {
            val token = mToken
            if (token != null) {
                val call = mApi.requestTransfer(token, taskInfo)
                val response = call.execute()
                val result = response.body()
                Logger.i(TAG, "requestCastTask: result = $result")
                if (result != null && result.errorCode == RESULT_SUCCESS) {

                }
            } else {
                Logger.e(TAG, "requestCastTask: invalid token")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "requestCastTask: error = ${e.message}")
        }
    }

    @WorkerThread
    private fun requestCastExit() {
        try {
            val token = mToken
            if (token != null) {
                val call = mApi.requestCastExit(token)
                val response = call.execute()
                val result = response.body()
                Logger.i(TAG, "requestCastTask: result = $result")
                if (result != null && result.errorCode == RESULT_SUCCESS) {

                }
            } else {
                Logger.e(TAG, "requestCastTask: invalid token")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "requestCastTask: error = ${e.message}")
        }
    }

    @WorkerThread
    private fun requestSendMessage(msg: Msg) {
        try {
            val token = mToken
            Logger.i(TAG, "requestSendMessage: token = $token")
            if (token != null) {
                val call = mApi.requestSendMessage(token, msg)
                val response = call.execute()
                val result = response.body()
                Logger.i(TAG, "requestSendMessage: result = $result")
                if (result != null && result.errorCode == RESULT_SUCCESS) {

                }
            } else {
                Logger.e(TAG, "requestSendMessage: invalid token")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "requestCastTask: error = ${e.message}")
        }
    }

    private fun runOnWorkerThread(r: () -> Unit) {
        mWorkerHandler.post(r)
    }

    fun close() {
        isConnected = false
        isActive = false
        mToken = null
        mServer.channel = null
    }
}