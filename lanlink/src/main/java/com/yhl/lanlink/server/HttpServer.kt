package com.yhl.lanlink.server

import android.os.Message
import android.os.Messenger
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.yhl.lanlink.*
import com.yhl.lanlink.data.ResultData
import com.yhl.lanlink.data.ServiceInfo
import com.yhl.lanlink.data.TaskInfo
import com.yhl.lanlink.util.getIPv4Address
import fi.iki.elonen.NanoHTTPD
import java.io.InputStreamReader


class HttpServer(var mConnectionManager: ConnectionManager): NanoHTTPD(MESSAGE_SERVER_PORT) {

    override fun serve(session: IHTTPSession?): Response {
        println("serve: thread = {${Thread.currentThread().name}}")
        val method = session?.method
        if (Method.POST == method) {
            return parseBody(session) ?: responseHello(session)
        }
        return responseHello(session)
    }

    private fun parseBody(session: IHTTPSession): Response? {
        return when(session.uri) {
            URI_MEDIA_TRANSFER -> mConnectionManager.parseMediaTransferBody(this, session)
            URI_CONNECTION_CONNECT -> mConnectionManager.parseClientConnect(this, session)
            URI_CONNECTION_DISCONNECT -> mConnectionManager.parseClientDisconnect(this, session)
            URI_CONNECTION_HEART_BEAT -> mConnectionManager.parseHeartbeat(this, session)
            else -> null
        }
    }

    //页面不存在，或者文件不存在时
    fun responseHello(session: IHTTPSession?): Response {
        val builder = StringBuilder()
        builder.append("<!DOCTYPE html><html><body>")
        builder.append("Hello World!! time = ${System.currentTimeMillis()}")
        builder.append("This message is from ${getIPv4Address()}")
        builder.append("</body></html>\n")
        return newFixedLengthResponse(builder.toString())
    }
}