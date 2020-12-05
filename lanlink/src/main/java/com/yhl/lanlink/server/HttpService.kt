package com.yhl.lanlink.server

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.yhl.lanlink.*
import com.yhl.lanlink.nsd.ServiceManager
import fi.iki.elonen.NanoHTTPD
import java.io.FileDescriptor
import java.io.PrintWriter

val TAG = HttpService::class.simpleName
class HttpService: Service() {
    private var mHttpServer: HttpServer? = null
    private var mFileServer: NanoHTTPD? = null

    override fun onBind(intent: Intent?): IBinder? {
        val serviceManager = ServiceManager.getInstance(this)
        startServer(serviceManager)
        return ServiceBinder(serviceManager)
    }

    private fun startServer(serviceManager: ServiceManager) {
        Log.i(TAG, "startServer: 0")
        if (mHttpServer == null) {
            mHttpServer = HttpServer(serviceManager.mConnectionManager)
            mHttpServer!!.start(30*1000)
        }

        if (mFileServer == null) {
            mFileServer = FileServer()
            mFileServer!!.start()
        }
        Log.i(TAG, "startServer: 1")
    }

    private fun stopServer() {
        Log.i(TAG, "stopServer: ")
        mHttpServer?.stop()
        mHttpServer = null
        mFileServer?.stop()
        mFileServer = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startServer(ServiceManager.getInstance(this))
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopServer()
    }
}