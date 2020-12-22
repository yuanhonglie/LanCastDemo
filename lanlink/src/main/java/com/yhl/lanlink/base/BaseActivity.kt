package com.yhl.lanlink.base

import androidx.appcompat.app.AppCompatActivity
import com.yhl.lanlink.LanLinkSender
import com.yhl.lanlink.ServiceInfo
import com.yhl.lanlink.interfaces.MessageListener

abstract class BaseActivity() : AppCompatActivity(), MessageListener {

    override fun onResume() {
        super.onResume()
        LanLinkSender.getInstance().setMessageListener(this)
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onReceive(serviceInfo: ServiceInfo, type: String, data: Any, resultCode: Int) {

    }
}