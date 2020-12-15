package com.yhl.lanlink

import android.content.Context
import com.yhl.lanlink.interfaces.*

class LanLinkSender private constructor(private val lanLink: LanLink) : ILinkSender by lanLink {

    override fun destroy() {
        lanLink.stopDiscovery()
        lanLink.destroy()
        instance = null
    }

    companion object {
        private var instance: LanLinkSender? = null
        fun initialize(c: Context): Boolean {
            return LanLink.initialize(c)
        }

        fun getInstance(): LanLinkSender {
            if (instance == null) {
                synchronized(LanLinkSender::class) {
                    if (instance == null) {
                        instance = LanLinkSender(LanLink.getInstance())
                    }
                }
            }
            return instance!!
        }
    }
}