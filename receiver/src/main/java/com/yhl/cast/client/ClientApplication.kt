package com.yhl.cast.client

import android.app.Application
import com.yhl.lanlink.LanLink
import com.yhl.lanlink.data.TaskInfoCodec

class ClientApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        LanLink.initialize(this)
        LanLink.getInstance().registerMessageCodec(TaskInfoCodec())
    }

}