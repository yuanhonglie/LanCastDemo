package com.yhl.lanlink.server

import android.os.Message
import android.os.Messenger
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.yhl.lanlink.*
import com.yhl.lanlink.data.ResultData
import com.yhl.lanlink.data.TaskInfo
import fi.iki.elonen.NanoHTTPD
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

class ConnectionManager(var mUiMessenger: Messenger? = null) {

    private var mGson: Gson = GsonBuilder().setLenient().create()

    /**
     * 与当前服务连接的client信息列表, token为key，serviceInfo为value
     */
    private val mClientMap = mutableMapOf<String, ServiceInfo>()

    /**
     * 与当前服务器建立连接client的token列表，serviceInfo#id为key，token为value
     */
    private val mTokenMap = mutableMapOf<String, String>()

    /**
     * 当前的有效token集合
     */
    private val mTokens = mutableSetOf<String>()

    /**
     * 与服务连接的client的活跃时间，token为key，当前时间值为value
     */
    private val mActiveTimes = mutableMapOf<String, Long>()

    fun parseMediaTransferBody(
        httpServer: HttpServer,
        session: NanoHTTPD.IHTTPSession
    ): NanoHTTPD.Response {
        val token = parseToken(session)
        return if (validateToken(token) || true) {
            println("parseMediaTransferBody: $mUiMessenger")
            val adapter = mGson.getAdapter(TypeToken.get(TaskInfo::class.java))
            val reader = InputStreamReader(session.inputStream)
            val taskInfo = adapter.fromJson(reader)
            val msg = Message.obtain().apply {
                what = 100
                obj = taskInfo
            }
            mUiMessenger?.send(msg)
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
        return if (validateToken(token) || true) {
            println("parseMediaTransferBody: $mUiMessenger")
            val msg = Message.obtain().apply {
                what = 101
            }
            mUiMessenger?.send(msg)
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
        val adapter = mGson.getAdapter(TypeToken.get(ServiceInfo::class.java))
        val reader = InputStreamReader(session.inputStream)
        val serviceInfo = adapter.fromJson(reader)
        val token = registerClient(serviceInfo)
        println("parseClientConnect: serviceInfo = $serviceInfo, token=$token")
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
    private fun registerClient(client: ServiceInfo): String {
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
            mTokenMap.remove(it.id)
        }
        mActiveTimes.remove(token)
        mTokens.remove(token)
    }

    /**
     * 校验Token是否合法
     */
    private fun validateToken(token: String): Boolean {
        return if (mTokens.contains(token)) {
            val lastTime = mActiveTimes[token] ?: 0
            val nowTime = System.currentTimeMillis()
            println("validateToken: timeElapsed = ${nowTime - lastTime}, token=$token")
            mActiveTimes[token] = nowTime
            true
        } else false
    }

    private fun parseToken(session: NanoHTTPD.IHTTPSession) = session.headers["token"] ?: ""
    private fun parseTimestamp(session: NanoHTTPD.IHTTPSession) = session.headers["timestamp"] ?: 0

    fun destroy() {
        mClientMap.clear()
        mTokenMap.clear()
        mTokens.clear()
        mActiveTimes.clear()
    }
}