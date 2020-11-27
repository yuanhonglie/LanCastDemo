package com.yhl.lanlink.server

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.yhl.lanlink.MSG_ACTIVITY_REGISTER
import fi.iki.elonen.NanoHTTPD

val TAG = HttpService::class.simpleName
class HttpService: Service() {
    var mHttpServer: HttpServer? = null
    var mFileServer: NanoHTTPD? = null

    val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when(msg.what) {
                MSG_ACTIVITY_REGISTER -> {
                    mClientMessenger = msg.replyTo
                    mHttpServer?.uiMessenger = mClientMessenger
                }
            }
        }
    }
    var mClientMessenger: Messenger? = null
    var mServerMessenger: Messenger = Messenger(handler)

    override fun onBind(intent: Intent?): IBinder? {
        startServer()
        return mServerMessenger.binder
    }

    private fun startServer() {
        Log.i(TAG, "startServer: ")
        if (mHttpServer == null) {
            mHttpServer = HttpServer()
            mHttpServer!!.start(30*1000)
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