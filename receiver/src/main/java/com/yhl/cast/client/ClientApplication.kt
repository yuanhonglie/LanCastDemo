package com.yhl.cast.client

import android.app.Application
import com.yhl.cast.client.data.UserCodec
import com.yhl.lanlink.LanLinkReceiver

class ClientApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        if (LanLinkReceiver.initialize(this)) {
            LanLinkReceiver.getInstance().registerMessageCodec(UserCodec())
        }
    }

}