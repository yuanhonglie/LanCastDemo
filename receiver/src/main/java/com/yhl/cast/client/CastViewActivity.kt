package com.yhl.cast.client

import android.content.Intent
import android.os.Bundle
import android.os.Message
import android.view.View
import com.bumptech.glide.Glide
import com.yhl.lanlink.base.BaseActivity
import com.yhl.lanlink.data.Media
import com.yhl.lanlink.data.MediaType
import com.yhl.lanlink.data.TaskInfo
import kotlinx.android.synthetic.main.activity_cast_view.*

const val KEY_MEDIA_INFO = "media"
const val KEY_CAST_EXIT = "exit"

class CastViewActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cast_view)
        ivBack.setOnClickListener {
            onBackPressed()
        }
        initData(intent)
    }

    private fun initData(intent: Intent) {
        val media = intent.getParcelableExtra<Media>(KEY_MEDIA_INFO)
        val exit = intent.getBooleanExtra(KEY_CAST_EXIT, false)
        println("initData: $media")
        if (media != null) {
            playMedia(media)
        }

        if (exit) {
            finish()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            initData(intent)
        }
    }

    private fun playMedia(media: Media) {
        if (media.mediaType == MediaType.video) {
            stopVideo()
            vvVideo.visibility = View.VISIBLE
            ivImage.visibility = View.GONE
            vvVideo.setVideoPath(media.uri)
            //vvVideo.setOnCompletionListener { vvVideo.start() }
            vvVideo.start()
        } else {
            ivImage.visibility = View.VISIBLE
            vvVideo.visibility = View.GONE
            Glide.with(this).load(media.uri).into(ivImage)
        }
    }

    private fun stopVideo() {
        if (vvVideo.isPlaying) {
            vvVideo.stopPlayback()
        }
    }

    override fun onMessage(msg: Message) {
        super.onMessage(msg)
        when (msg.what) {
            100 -> {
                val taskInfo = msg.obj
                println("onMessage: taskInfo = $taskInfo")
                if (taskInfo is TaskInfo) {
                    val media = taskInfo.media
                    playMedia(media)
                }
            }
            101 -> {
                onBackPressed()
            }
        }
    }


}