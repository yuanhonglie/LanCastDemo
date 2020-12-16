package com.yhl.lanlink.server

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

val TAG = HttpService::class.simpleName
class HttpService: Service() {
    private var mHttpServer: HttpServer? = null
    private var mFileServer: FileServer? = null

    override fun onCreate() {
        super.onCreate()
        println("onCreate: ")
    }

    override fun onBind(intent: Intent?): IBinder? {
        println("onBind: ")
        val serviceManager = ServiceManager.getInstance(this)
        startServer(serviceManager)
        return serviceManager
    }

    private fun startServer(serviceManager: ServiceManager) {
        Log.i(TAG, "startServer: 0")
        if (mHttpServer == null) {
            mHttpServer = HttpServer(serviceManager.mConnectionManager)
            mHttpServer!!.start(30*1000)
        }

        if (mFileServer == null) {
            mFileServer = FileServer(this, serviceManager.mConnectionManager)
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

    fun getServeFilePath(path: String): String? {
        return mFileServer?.serveFile(path) ?: ""
    }

    override fun onUnbind(intent: Intent?): Boolean {
        println("onUnbind:")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        println("onDestroy: ")
        stopServer()
        ServiceManager.getInstance(this).onDestroy()
    }
}