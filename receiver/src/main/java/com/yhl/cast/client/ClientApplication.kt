package com.yhl.cast.client

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.yhl.cast.client.data.HelloCodecGson
import com.yhl.cast.client.data.UserCodecProto
import com.yhl.lanlink.LanLinkReceiver
import com.yhl.lanlink.LanLinkSender

class ClientApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        if (LanLinkReceiver.initialize(this)) {
            LanLinkReceiver.getInstance().registerMessageCodec(UserCodecProto())
            LanLinkSender.getInstance().registerMessageCodec(HelloCodecGson())
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

}