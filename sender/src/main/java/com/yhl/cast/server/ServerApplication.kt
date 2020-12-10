package com.yhl.cast.server

import android.app.Application
import com.yhl.cast.server.data.UserCodec
import com.yhl.lanlink.LanLink

class ServerApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        LanLink.initialize(this)
        LanLink.getInstance().registerMessageCodec(UserCodec())
    }

}