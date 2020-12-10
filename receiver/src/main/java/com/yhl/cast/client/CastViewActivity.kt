package com.yhl.cast.client

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import com.bumptech.glide.Glide
import com.yhl.lanlink.ServiceInfo
import com.yhl.lanlink.base.BaseActivity
import com.yhl.lanlink.data.*
import kotlinx.android.synthetic.main.activity_cast_view.*

const val KEY_MEDIA_INFO = "media"
const val KEY_MEDIA_PATH = "path"
const val KEY_MEDIA_TYPE = "type"

class CastViewActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cast_view)
        backLayout.setOnClickListener {
            onBackPressed()
        }
        initData(intent)
    }

    private fun initData(intent: Intent) {
        val media = intent.getParcelableExtra<Media>(KEY_MEDIA_INFO)
        val path = intent.getStringExtra(KEY_MEDIA_PATH)
        val mediaType = when(intent.getIntExtra(KEY_MEDIA_TYPE, 0)) {
            0 -> MediaType.image
            1 -> MediaType.video
            else -> MediaType.stream
        }
        println("initData: $media")
        if (media != null) {
            playMedia(media)
        }

        if (TextUtils.isEmpty(path).not()) {
            playMedia(path, mediaType)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            initData(intent)
        }
    }

    private fun playMedia(media: Media) {
        playMedia(media.uri, media.mediaType)
    }

    private fun playMedia(path: String, type: MediaType) {
        if (type == MediaType.video) {
            stopVideo()
            vvVideo.visibility = View.VISIBLE
            ivImage.visibility = View.GONE
            vvVideo.setVideoPath(path)
            //vvVideo.setOnCompletionListener { vvVideo.start() }
            vvVideo.start()
        } else {
            ivImage.visibility = View.VISIBLE
            vvVideo.visibility = View.GONE
            Glide.with(this).load(path).into(ivImage)
        }
    }

    private fun stopVideo() {
        if (vvVideo.isPlaying) {
            vvVideo.stopPlayback()
        }
    }

    override fun onMessage(serviceInfo: ServiceInfo, type: String, data: Any) {
        super.onMessage(serviceInfo, type, data)
        when (type) {
            TaskInfo::class.qualifiedName -> {
                if (data is TaskInfo) {
                    if (data.actionType == ActionType.cast) {
                        playMedia(data.media)
                    }
                }
            }
            ControlInfo::class.qualifiedName -> {
                if (data is ControlInfo) {
                    if (data.control == 1) {
                        onBackPressed()
                    }
                }
            }
        }
    }
}