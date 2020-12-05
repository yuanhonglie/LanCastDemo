package com.yhl.cast.client

import android.app.Application
import com.yhl.lanlink.LanLink

class ClientApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        LanLink.initialize(this)
    }

}