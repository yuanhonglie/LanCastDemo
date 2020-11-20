package com.yhl.server

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import fi.iki.elonen.NanoHTTPD

val TAG = HttpService::class.simpleName

class HttpService: Service() {
    var mHttpServer: NanoHTTPD? = null
    var mFileServer: NanoHTTPD? = null

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startServer() {
        Log.i(TAG, "startServer: ")
        if (mHttpServer == null) {
            mHttpServer = HttpServer()
            mHttpServer!!.start()
        }

        if (mFileServer == null) {
            mFileServer = FileServer()
            mFileServer!!.start()
        }
    }

    private fun stopServer() {
        Log.i(TAG, "stopServer: ")
        mHttpServer?.stop()
        mHttpServer = null
        mFileServer?.stop()
        mFileServer = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startServer()
        return START_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
        stopServer()
    }
}