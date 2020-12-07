package com.yhl.cast.server

import android.content.Intent
import android.os.Bundle
import com.yhl.cast.server.albumpicker.model.AlbumFile
import com.yhl.lanlink.ServiceInfo
import com.yhl.lanlink.base.BaseActivity
import kotlinx.android.synthetic.main.activity_media_type.*
import kotlinx.android.synthetic.main.layout_lv_footer.*
import kotlinx.android.synthetic.main.layout_lv_header.*

class CastMediaActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_type)
        btnImage.setOnClickListener {
            startMediaCastActivity(AlbumFile.TYPE_IMAGE)
        }
        btnVideo.setOnClickListener {
            startMediaCastActivity(AlbumFile.TYPE_VIDEO)
        }
        ivBack.setOnClickListener{ onBackPressed() }
        tvVersion.setText("Demo V1.0")
    }

    private fun startMediaCastActivity(type: Int) {
        val intent = Intent()
        intent.setClass(this, AlbumPickActivity::class.java)
        val serviceInfo = getIntent().getParcelableExtra(SERVICE_INFO) as ServiceInfo
        intent.putExtra(SERVICE_INFO, serviceInfo)
        intent.putExtra(MEDIA_TYPE, type)
        startActivity(intent)
    }

}