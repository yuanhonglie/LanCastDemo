package com.yhl.lanlink.server

import com.yhl.lanlink.*
import com.yhl.lanlink.util.getIPv4Address
import fi.iki.elonen.NanoHTTPD


class HttpServer(private val mConnectionManager: ConnectionManager): NanoHTTPD(MESSAGE_SERVER_PORT) {

    private val TAG = "HttpServer"
    override fun serve(session: IHTTPSession?): Response {
        val method = session?.method
        if (Method.POST == method) {
            return parseBody(session) ?: responseHello(session)
        }
        return responseHello(session)
    }

    private fun parseBody(session: IHTTPSession): Response? {
        return when(session.uri) {
            URI_CAST_TRANSFER -> mConnectionManager.parseMediaTransferBody(this, session)
            URI_CAST_EXIT -> mConnectionManager.parseMediaCastExitBody(this, session)
            URI_SEND_MESSAGE -> mConnectionManager.parseSendMessageBody(this, session)
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