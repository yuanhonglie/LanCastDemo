package com.yhl.server

import com.yhl.util.getIPv4Address
import fi.iki.elonen.NanoHTTPD

const val MESSAGE_SERVER_PORT = 8030
class HttpServer: NanoHTTPD(MESSAGE_SERVER_PORT) {

    override fun serve(session: IHTTPSession?): Response {
        println("serve: thread = {${Thread.currentThread().name}}")
        return responseHello(session)
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