package com.yhl.lanlink.server

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.yhl.lanlink.*
import com.yhl.lanlink.channel.Channel
import com.yhl.lanlink.data.ControlInfo
import com.yhl.lanlink.data.ResultData
import com.yhl.lanlink.data.TaskInfo
import com.yhl.lanlink.log.Logger
import fi.iki.elonen.NanoHTTPD
import okio.Buffer
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import java.util.*

class ConnectionManager(private val mServiceManager: ServiceManager) {

    private val TAG = "ConnectionManager"

    private var mGson: Gson = GsonBuilder().setLenient().create()

    /**
     * 当前的有效token集合，这里的token包括：
     * 1）作为客户端，与服务端建立连接之后，后续服务端与客户端发送消息通信需要用token，此token在客户端连接服务端时，直接传递给服务端。
     * 2）作为服务端，响应客户端的请求连接时，生成一个token。后续客户端与服务端之间的通信，如发送消息、发送心跳、请求断开连接等，都是通过这个token作为身份校验的。
     */
    private val mTokens = mutableSetOf<String>()

    /**
     * 与当前服务连接的client信息列表, token为key，serviceInfo为value
     */
    private val mClientMap = mutableMapOf<String, ServiceInfo>()

    /**
     * 与当前服务器建立连接client的token列表，serviceInfo#id为key，token为value
     */
    private val mTokenMap = mutableMapOf<String, String>()

    /**
     * 作为服务端，记录与之连接的client的最近活跃时间，token为key，时间值为value
     */
    private val mActiveTimes = mutableMapOf<String, Long>()

    fun parseMediaTransferBody(
        httpServer: HttpServer,
        session: NanoHTTPD.IHTTPSession
    ): NanoHTTPD.Response {
        val token = parseToken(session)
        return if (validateToken(token)) {

            val adapter = mGson.getAdapter(TaskInfo::class.java)
            val reader = InputStreamReader(session.inputStream)
            val value = adapter.fromJson(reader)

            val buffer = Buffer()
            val writer = OutputStreamWriter(buffer.outputStream(), Charset.forName("UTF-8"))
            writer.use {
                val jsonWriter = mGson.newJsonWriter(writer)
                adapter.write(jsonWriter, value)
                jsonWriter.close()
            }

            val msg = Msg(TaskInfo::class.qualifiedName ?: "TaskInfo", buffer.readByteArray())
            val serviceInfo = mClientMap[token]
            if (serviceInfo != null) {
                mServiceManager.onReceiveMessage(serviceInfo, msg)
            }

            newSimpleResultDataResponse(RESULT_SUCCESS, RESULT_MESSAGE_SUCCESS)
        } else {
            newSimpleResultDataResponse(RESULT_FAILED, RESULT_MESSAGE_INVALID_TOKEN)
        }
    }

    fun parseMediaCastExitBody(
        httpServer: HttpServer,
        session: NanoHTTPD.IHTTPSession
    ): NanoHTTPD.Response {
        val token = parseToken(session)
        return if (validateToken(token)) {

            val adapter = mGson.getAdapter(ControlInfo::class.java)
            val value = ControlInfo(CONTROL_EXIT_CAST)

            val buffer = Buffer()
            val writer = OutputStreamWriter(buffer.outputStream(), Charset.forName("UTF-8"))
            writer.use {
                val jsonWriter = mGson.newJsonWriter(writer)
                adapter.write(jsonWriter, value)
                jsonWriter.close()
            }

            val msg = Msg(ControlInfo::class.qualifiedName ?: "ControlInfo", buffer.readByteArray())
            val serviceInfo = mClientMap[token]
            if (serviceInfo != null) {
                mServiceManager.onReceiveMessage(serviceInfo, msg)
            }
            newSimpleResultDataResponse(RESULT_SUCCESS, RESULT_MESSAGE_SUCCESS)
        } else {
            newSimpleResultDataResponse(RESULT_FAILED, RESULT_MESSAGE_INVALID_TOKEN)
        }
    }

    fun parseSendMessageBody(
        httpServer: HttpServer,
        session: NanoHTTPD.IHTTPSession
    ): NanoHTTPD.Response {
        val token = parseToken(session)
        return if (validateToken(token)) {
            val adapter = mGson.getAdapter(Msg::class.java)
            val reader = InputStreamReader(session.inputStream)
            val msg = adapter.fromJson(reader)
            val serviceInfo = mClientMap[token]
            if (serviceInfo != null) {
                mServiceManager.onReceiveMessage(serviceInfo, msg)
            }
            newSimpleResultDataResponse(RESULT_SUCCESS, RESULT_MESSAGE_SUCCESS)
        } else {
            newSimpleResultDataResponse(RESULT_FAILED, RESULT_MESSAGE_INVALID_TOKEN)
        }
    }


    /**
     * 解析心跳包
     * @param session
     */
    fun parseHeartbeat(
        httpServer: HttpServer,
        session: NanoHTTPD.IHTTPSession
    ): NanoHTTPD.Response? {
        val token = parseToken(session)
        val timestamp = parseTimestamp(session)
        return if (validateToken(token)) {
            newResultDataResponse(RESULT_SUCCESS, RESULT_MESSAGE_SUCCESS, timestamp)
        } else {
            newSimpleResultDataResponse(RESULT_FAILED, RESULT_MESSAGE_INVALID_TOKEN)
        }
    }

    /**
     * 解析连接请求
     */
    fun parseClientConnect(
        httpServer: HttpServer,
        session: NanoHTTPD.IHTTPSession
    ): NanoHTTPD.Response? {
        val adapter = mGson.getAdapter(TypeToken.get(ClientInfo::class.java))
        val reader = InputStreamReader(session.inputStream)
        val clientInfo = adapter.fromJson(reader)
        val token = registerClient(clientInfo)
        Logger.i(TAG, "parseClientConnect: clientToken = ${clientInfo.token}")
        if (clientInfo.token.isNullOrEmpty().not()) {
            clientInfo.channel = Channel(this, mServiceManager.mWorkerHandler, clientInfo, clientInfo.token)
        }
        //通知客户端连接
        mServiceManager.notifyClientConnected(clientInfo, RESULT_SUCCESS)
        Logger.i(TAG, "parseClientConnect: clientInfo = $clientInfo, token=$token")
        return newResultDataResponse(RESULT_SUCCESS, RESULT_MESSAGE_SUCCESS, token)
    }

    /**
     * 解析断开请求
     */
    fun parseClientDisconnect(
        httpServer: HttpServer,
        session: NanoHTTPD.IHTTPSession
    ): NanoHTTPD.Response? {
        val token = parseToken(session)
        return if (validateToken(token)) {

            //通知客户端断开
            val clientInfo = mClientMap[token]
            if (clientInfo != null) {
                mServiceManager.notifyClientDisconnected(clientInfo, RESULT_SUCCESS)
            }
            unregisterClient(token)
            newSimpleResultDataResponse(RESULT_SUCCESS, RESULT_MESSAGE_SUCCESS)
        } else {
            newSimpleResultDataResponse(RESULT_FAILED, RESULT_MESSAGE_INVALID_TOKEN)
        }
    }

    private fun <T> newResultDataResponse(
        errorCode: Int,
        errorMessage: String,
        data: T
    ): NanoHTTPD.Response {
        val result = ResultData(errorCode, errorMessage, System.currentTimeMillis(), data)
        return NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK,
            MIME_TYPE_JSON,
            mGson.toJson(result)
        )
    }

    private fun newSimpleResultDataResponse(
        errorCode: Int,
        errorMessage: String
    ): NanoHTTPD.Response {
        return newResultDataResponse(errorCode, errorMessage, "")
    }

    /**
     * 注册客户端信息
     */
    fun registerClient(client: ServiceInfo): String {
        return if (mTokenMap.containsKey(client.id)) {
            mTokenMap[client.id]!!
        } else {
            val token = UUID.randomUUID().toString()
            mClientMap[token] = client
            mActiveTimes[token] = System.currentTimeMillis()
            mTokenMap[client.id] = token
            mTokens.add(token)
            token
        }
    }

    /**
     * 注销客户端信息
     */
    private fun unregisterClient(token: String) {
        mClientMap.remove(token)?.let {
            it.channel?.close()
            it.channel = null
            mTokenMap.remove(it.id)
        }
        mActiveTimes.remove(token)
        mTokens.remove(token)
    }

    /**
     * 注销客户端信息
     */
    fun unregisterClient(serviceInfo: ServiceInfo) {
        val token = mTokenMap[serviceInfo.id]
        if (token != null) {
            unregisterClient(token)
        }
    }

    /**
     * 校验Token是否合法
     */
    private fun validateToken(token: String): Boolean {
        return if (mTokens.contains(token)) {
            val lastTime = mActiveTimes[token] ?: 0
            val nowTime = System.currentTimeMillis()
            Logger.i(TAG, "validateToken: timeElapsed = ${nowTime - lastTime}, token=$token")
            mActiveTimes[token] = nowTime
            true
        } else {
            Logger.i(TAG, "validateToken: invalid token = $token")
            false
        }
    }

    private fun parseToken(session: NanoHTTPD.IHTTPSession) = session.headers["token"] ?: ""

    private fun parseTimestamp(session: NanoHTTPD.IHTTPSession) = session.headers["timestamp"] ?: 0

    /**
     * 判断此token是否为当前作为服务端，客户端请求连接时，为客户端生成的token
     */
    private fun isClientToken(token: String) = mClientMap.containsKey(token)

    fun performClientsAliveCheck(): Boolean {
        val nowTime = System.currentTimeMillis()
        for ((token, time) in mActiveTimes) {
            if (isClientToken(token)) {
                if (nowTime - time > CLIENT_TIMEOUT) {
                    val clientInfo = mClientMap[token]
                    Logger.i(TAG, "performClientsAliveCheck: client = $clientInfo time out!")
                    if (clientInfo != null) {
                        mServiceManager.notifyClientDisconnected(clientInfo, RESULT_FAILED_SENDER_TIMEOUT)
                    }
                    unregisterClient(token)
                }
            }
        }

        return mClientMap.isNotEmpty()
    }

    fun destroy() {
        mClientMap.clear()
        mTokenMap.clear()
        mTokens.clear()
        mActiveTimes.clear()
    }
}