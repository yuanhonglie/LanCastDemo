package com.yhl.lanlink.server

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.yhl.lanlink.log.Logger

class HttpService: Service() {
    private val TAG = "HttpService"
    private var mHttpServer: HttpServer? = null
    private var mFileServer: FileServer? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate: ")
    }

    override fun onBind(intent: Intent?): IBinder? {
        Logger.i(TAG, "onBind: ")
        val serviceManager = ServiceManager.getInstance(this)
        startServer(serviceManager)
        return serviceManager
    }

    private fun startServer(serviceManager: ServiceManager) {
        if (mHttpServer == null) {
            mHttpServer = HttpServer(serviceManager.mConnectionManager)
            mHttpServer!!.start(30*1000)
        }

        if (mFileServer == null) {
            mFileServer = FileServer(this, serviceManager.mConnectionManager)
            mFileServer!!.start()
        }
    }

    private fun stopServer() {
        mHttpServer?.stop()
        mHttpServer = null
        mFileServer?.stop()
        mFileServer = null
    }

    fun getServeFilePath(path: String): String? {
        return mFileServer?.serveFile(path) ?: ""
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopServer()
        ServiceManager.getInstance(this).onDestroy()
    }
}