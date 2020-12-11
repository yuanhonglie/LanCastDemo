package com.yhl.lanlink

import android.content.Context
import com.yhl.lanlink.interfaces.*

class LanLinkReceiver private constructor(private val lanLink: LanLink) : ILinkReceiver by lanLink {

    companion object {
        private var instance: LanLinkReceiver? = null
        fun initialize(c: Context): Boolean {
            return LanLink.initialize(c)
        }

        fun getInstance(): LanLinkReceiver {
            if (instance == null) {
                synchronized(LanLinkReceiver::class) {
                    if (instance == null) {
                        instance = LanLinkReceiver(LanLink.getInstance())
                    }
                }
            }
            return instance!!
        }
    }
}