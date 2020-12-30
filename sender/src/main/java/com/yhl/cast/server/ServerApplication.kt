package com.yhl.cast.server

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.yhl.cast.server.data.HelloCodecGson
import com.yhl.cast.server.data.UserCodec
import com.yhl.lanlink.LanLinkSender

class ServerApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        if (LanLinkSender.initialize(this)) {
            LanLinkSender.getInstance().registerMessageCodec(UserCodec())
            LanLinkSender.getInstance().registerMessageCodec(HelloCodecGson())
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}