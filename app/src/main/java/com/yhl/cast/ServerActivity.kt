package com.yhl.cast

import android.os.Bundle
import com.yhl.data.MediaType
import kotlinx.android.synthetic.main.activity_server.*

class ServerActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)

        btnSendImage.setOnClickListener {
            sendCastTask(getFileServerUrl() + getImageUri(), MediaType.image)
        }

        btnSendVideo.setOnClickListener {
            sendCastTask(getFileServerUrl() + getVideoUri(), MediaType.video)
        }
    }

    private fun getImageUri() = if (etImagePath.text.isEmpty().not()) etImagePath.text.toString() else "/sdcard/media/image1.jpg"
    private fun getVideoUri() = if (etVideoPath.text.isEmpty().not()) etVideoPath.text.toString() else "/sdcard/media/video03.jpg"

    override fun getBaseUrl() = getMessageServerUrl()

    override fun getHost() = "192.168.12.161"
}