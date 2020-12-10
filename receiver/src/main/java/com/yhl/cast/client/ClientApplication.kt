package com.yhl.cast.client

import android.app.Application
import com.yhl.cast.client.data.UserCodec
import com.yhl.lanlink.LanLink
import com.yhl.lanlink.data.ControlInfo
import com.yhl.lanlink.data.ControlInfoCodec
import com.yhl.lanlink.data.TaskInfoCodec

class ClientApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        LanLink.initialize(this)
        LanLink.getInstance().registerMessageCodec(UserCodec())
    }

}