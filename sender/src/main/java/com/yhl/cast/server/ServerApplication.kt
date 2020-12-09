package com.yhl.cast.server

import android.app.Application
import com.yhl.lanlink.LanLink
import com.yhl.lanlink.data.TaskInfoCodec

class ServerApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        LanLink.initialize(this)
        LanLink.getInstance().registerMessageCodec(TaskInfoCodec())
    }

}