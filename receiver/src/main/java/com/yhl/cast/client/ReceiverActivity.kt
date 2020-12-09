package com.yhl.cast.client

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Message
import android.os.Process
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yhl.lanlink.LanLink.Companion.getInstance
import com.yhl.lanlink.base.BaseActivity
import com.yhl.lanlink.data.ActionType
import com.yhl.lanlink.data.Media
import com.yhl.lanlink.data.MediaType
import com.yhl.lanlink.data.TaskInfo
import kotlinx.android.synthetic.main.activity_receiver.*
import kotlinx.android.synthetic.main.layout_lv_header.*
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit

private const val STATUS_DOWNLOADING = 1
private const val STATUS_DOWNLOADED = 2

class ReceiverActivity : BaseActivity(), View.OnClickListener, OnItemClickListener, DownloadProgressListener {
    private var mDeviceName: String? = null
    private lateinit var mAdapter: MediaAdapter
    private lateinit var mDownloader: Downloader
    private val mediaMap = mutableMapOf<String, MediaItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receiver)
        initView()
        //checkPermission()
        mDownloader = Downloader(this)
    }

    private fun initCastServer() {}
    private fun initView() {
        ivBack.setOnClickListener { this }
        btnStop.setOnClickListener { this }
        btnStart.setOnClickListener(this)

        mAdapter = MediaAdapter(this)
        mAdapter.onItemClickListener = this
        lvMedia.adapter = mAdapter
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnStart -> {
                btnStart.isSelected = true
                startService()
            }
            R.id.btnStop -> stopService()
            R.id.ivBack -> onBackPressed()
            else -> {
            }
        }
    }

    private val deviceName: String?
        private get() {
            if (TextUtils.isEmpty(mDeviceName)) {
                mDeviceName = "$SERVICE_NAME-$timeStamp"
            }
            return mDeviceName
        }

    private val timeStamp: String
        private get() {
            val timeText = "" + System.currentTimeMillis()
            val length = timeText.length
            val index =
                if (length > SUFFIX_LENGTH) length - SUFFIX_LENGTH else 0
            return timeText.substring(index)
        }

    private fun startService() {
        getInstance().registerService(deviceName!!)
    }

    private fun stopService() {
        getInstance().unregisterService()
    }

    private fun checkPermission() {
        val permissionsCheck = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )
        val permissionList: MutableList<String> = ArrayList()
        for (permissionStr in permissionsCheck) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permissionStr
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionList.add(permissionStr)
            }
        }

        if (permissionList.size > 0) {
            ActivityCompat.requestPermissions(
                this,
                permissionList.toTypedArray(),
                100
            )
            return
        }
        initCastServer()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != 100) {
            println("onRequestPermissionsResult: $requestCode")
            return
        }
        if (grantResults.size <= 0) {
            println("onRequestPermissionsResult: ${grantResults.size}")
            return
        }
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            println("onRequestPermissionsResult: NOT PERMISSION_GRANTED")
            return
        }
        initCastServer()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Process.killProcess(Process.myPid())
        System.exit(0)
    }

    override fun onMessage(msg: Message) {
        super.onMessage(msg)
        when (msg.what) {
            100 -> {
                val taskInfo = msg.obj
                println("onMessage: taskInfo = $taskInfo")
                if (taskInfo is TaskInfo) {
                    if (taskInfo.actionType == ActionType.cast) {
                        val media = taskInfo.media
                        val intent = Intent(this, CastViewActivity::class.java)
                        intent.putExtra(KEY_MEDIA_INFO, media)
                        startActivity(intent)
                    } else if (taskInfo.actionType == ActionType.store) {
                        val mediaItem = MediaItem(taskInfo.media)
                        startDownload(mediaItem)
                        mAdapter.add(mediaItem)
                    }
                }
            }
        }
    }

    private fun startDownload(mediaItem: MediaItem) {
        mediaMap.put(mediaItem.media.uri, mediaItem)
        val request = Request.Builder()
            .url(mediaItem.media.uri)
            .build()
        val mediaDir = when (mediaItem.media.mediaType) {
            MediaType.video -> "media${File.separator}video"
            MediaType.image -> "media${File.separator}image"
            else -> ""
        }

        val dir = File(filesDir, mediaDir)
        val filePath = File(dir, mediaItem.name)
        mediaItem.filePath = filePath.absolutePath
        println("startDownload: path = ${mediaItem.filePath}")
        try {
            mDownloader.client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    println("startDownload -> onFailure: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body
                    val inputStream = body?.byteStream()
                    if (inputStream != null) {
                        try {
                            writeFile(inputStream, filePath)
                        } catch (e: Exception) {
                            println("startDownload -> onResponse: ${e.message}")
                        }
                    }
                }

            })
        } catch (e: Exception) {
            println("startDownload: ${e.message}")
        }
    }

    private fun writeFile(inputStream: InputStream, file: File) {
        if (file.parentFile.exists().not()) {
            file.parentFile.mkdirs()
        }

        val outputStream = FileOutputStream(file)
        val buffer = ByteArray(1024 * 128)
        var len = -1
        while (inputStream.read(buffer).also({ len = it }) != -1) {
            outputStream.write(buffer, 0, len)
        }
        outputStream.flush()
        outputStream.close()
        inputStream.close()
    }

    class MediaItem(val media: Media) {
        val name = media.name
        var filePath: String? = null
        var status = 0
        var size = 0
        var progress = 0
        var speed = 0f
    }

    class MediaAdapter(private val context: Context): BaseAdapter() {
        var mediaList = mutableListOf<MediaItem>()
        var onItemClickListener: OnItemClickListener? = null

        fun add(item: MediaItem) {
            mediaList.add(item)
            notifyDataSetChanged()
        }

        fun remove(item: MediaItem) {
            mediaList.remove(item)
            notifyDataSetChanged()
        }

        fun clear() {
            mediaList.clear()
            notifyDataSetChanged()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            println("getView:")
            val view: View?
            val viewHolder: ViewHolder
            if (convertView == null) {
                view = LayoutInflater.from(context).inflate(R.layout.layout_media_item, null)
                viewHolder = ViewHolder(view)
                view?.tag = viewHolder
            } else {
                view = convertView
                viewHolder = view.tag as ViewHolder
            }

            //viewHolder.root?.setTag(position)
            val mediaItem = mediaList[position]
            viewHolder.name?.text = mediaItem.name
            viewHolder.status?.text = when(mediaItem.status) {
                STATUS_DOWNLOADING -> "下载中"
                STATUS_DOWNLOADED -> "已下载"
                else -> ""
            }

            println("getView: progress=${viewHolder.progress}")
            viewHolder.size?.text = sizeText(mediaItem.size.toFloat())
            viewHolder.status?.text = "${sizeText(mediaItem.speed)}/s"
            viewHolder.progress?.setProgress(mediaItem.progress)
            viewHolder.progress?.setOnClickListener {
                println("getView: setOnClickListener")
                val mediaItem = mediaList[position]
                if (mediaItem.status == STATUS_DOWNLOADED) {
                    onItemClickListener?.onClick(position, mediaList[position])
                }
            }

            return view as View
        }

        val SIZE_UNIT = arrayOf("B", "KB", "MB", "GB")
        private fun sizeText(size: Float): String {
            var unit = 0

            var remain = size
            while (remain > 1024) {
                unit++
                remain /= 1024
            }

            val format = DecimalFormat(".##")
            return "${format.format(remain)} ${SIZE_UNIT[unit]}"
        }

        override fun getItem(position: Int) = mediaList.get(position)

        override fun getItemId(position: Int) = position.toLong()

        override fun getCount() = mediaList.size

        class ViewHolder(view: View?) {
            var name: TextView? = null
            var status: TextView? = null
            var size: TextView? = null
            var progress: ProgressBar? = null

            init {
                name = view?.findViewById(R.id.tvName)
                status = view?.findViewById(R.id.tvStatus)
                size = view?.findViewById(R.id.tvFileSize)
                progress = view?.findViewById(R.id.pbProgress)
            }
        }
    }

    companion object {
        private const val TAG = "ReceiverActivity"
        private const val SERVICE_NAME = "DIDA Handbag"
        private const val SUFFIX_LENGTH = 5
    }

    override fun onClick(position: Int, item: MediaItem) {
        println("onClick: item = ${item.filePath}")
        val intent = Intent(this, CastViewActivity::class.java)
        intent.putExtra(KEY_MEDIA_PATH, item.filePath)
        intent.putExtra(KEY_MEDIA_TYPE, item.media.mediaType.value)
        startActivity(intent)
    }

    override fun update(url: String, bytesRead: Long, contentLength: Long, done: Boolean, duration: Long) {
        val mediaItem = mediaMap[url]
        if (mediaItem != null) {
            runOnUiThread {
                println("update url=$url, mediaItem=$mediaItem, bytesRead=$bytesRead")
                mediaItem.size = contentLength.toInt()
                mediaItem.progress = if (done) 100 else ((bytesRead * 100) / contentLength).toInt()
                mediaItem.status = if (done) STATUS_DOWNLOADED else STATUS_DOWNLOADING
                mediaItem.speed = if (duration == 0L) 0f else bytesRead * 1000f / duration
                mAdapter.notifyDataSetChanged()
            }
        }


    }
}

interface OnItemClickListener {
    fun onClick(position: Int, item: ReceiverActivity.MediaItem)
}

class Downloader(listener: DownloadProgressListener) {
    private val TIME_OUT = 15L
    var client: OkHttpClient
    init {
        val builder = OkHttpClient.Builder()
        builder.connectTimeout(TIME_OUT, TimeUnit.SECONDS)
            .writeTimeout(TIME_OUT, TimeUnit.SECONDS)
            .readTimeout(TIME_OUT, TimeUnit.SECONDS)
            .addInterceptor(DownloadProgressInterceptor(listener))

        client = builder.build()
    }
}