package com.yhl.server

import android.os.Message
import android.os.Messenger
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.yhl.data.TaskInfo
import com.yhl.http.URI_MEDIA_TRANSFER
import com.yhl.util.getIPv4Address
import fi.iki.elonen.NanoHTTPD
import java.io.InputStreamReader

const val MESSAGE_SERVER_PORT = 8030
class HttpServer(var uiMessenger: Messenger? = null): NanoHTTPD(MESSAGE_SERVER_PORT) {
    var gson: Gson
    init {
        gson = GsonBuilder().setLenient().create()
    }

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
            URI_MEDIA_TRANSFER -> parseMediaTransferBody(session)
            else -> null
        }
    }

    private fun parseMediaTransferBody(session: IHTTPSession): Response {
        val adapter = gson.getAdapter(TypeToken.get(TaskInfo::class.java))
        val reader = InputStreamReader(session.inputStream)
        val taskInfo = adapter.fromJson(reader)
        val msg = Message.obtain().apply {
            what = 100
            obj = taskInfo
        }
        uiMessenger?.send(msg)
        println("taskInfo = ${taskInfo}")
        return newFixedLengthResponse("success!")
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