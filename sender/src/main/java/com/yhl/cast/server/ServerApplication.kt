package com.yhl.cast.server

import android.app.Application
import com.yhl.cast.server.data.UserCodec
import com.yhl.lanlink.LanLinkSender

class ServerApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        if (LanLinkSender.initialize(this)) {
            LanLinkSender.getInstance().registerMessageCodec(UserCodec())
        }
    }

}