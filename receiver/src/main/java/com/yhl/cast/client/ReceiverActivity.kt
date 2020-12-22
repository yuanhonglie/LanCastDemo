package com.yhl.cast.client

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Process
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yhl.cast.client.data.Hello
import com.yhl.cast.client.data.User
import com.yhl.lanlink.LanLinkReceiver
import com.yhl.lanlink.RESULT_SUCCESS
import com.yhl.lanlink.ServiceInfo
import com.yhl.lanlink.base.BaseActivity
import com.yhl.lanlink.data.ActionType
import com.yhl.lanlink.data.Media
import com.yhl.lanlink.data.MediaType
import com.yhl.lanlink.data.TaskInfo
import com.yhl.lanlink.interfaces.ConnectionListener
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

class ReceiverActivity : BaseActivity(), OnItemClickListener, DownloadProgressListener {
    private var mDeviceName: String? = null
    private lateinit var mAdapter: MediaAdapter
    private lateinit var mDownloader: Downloader
    private val mediaMap = mutableMapOf<String, MediaItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receiver)
        initView()
        //checkPermission()
        initLanLinkReceiver()
        mDownloader = Downloader(this)
    }

    private fun initLanLinkReceiver() {
        LanLinkReceiver.getInstance().setConnectionListener(object : ConnectionListener {
            override fun onConnect(serviceInfo: ServiceInfo, resultCode: Int) {
                println("onConnect: $serviceInfo")
                LanLinkReceiver.getInstance().sendMessage(serviceInfo, Hello("Hello, ${serviceInfo.name}, I'm receiver"), "hello-msg")
            }

            override fun onDisconnect(serviceInfo: ServiceInfo, resultCode: Int) {
                println("onDisconnect: $serviceInfo")
            }

        })
    }
    private fun initView() {
        ivBack.setOnClickListener {
            onBackPressed()
        }
        btnStop.setOnClickListener {
            btnStart.isSelected = false
            stopService()
        }
        btnStart.setOnClickListener {
            btnStart.isSelected = true
            startService()
        }

        mAdapter = MediaAdapter(this)
        mAdapter.onItemClickListener = this
        lvMedia.adapter = mAdapter
    }

    private val deviceName: String?
        get() {
            if (TextUtils.isEmpty(mDeviceName)) {
                mDeviceName = "$SERVICE_NAME-8888"
            }
            return mDeviceName
        }

    private val timeStamp: String
        get() {
            val timeText = "" + System.currentTimeMillis()
            val length = timeText.length
            val index =
                if (length > SUFFIX_LENGTH) length - SUFFIX_LENGTH else 0
            return timeText.substring(index)
        }

    private fun startService() {
        LanLinkReceiver.getInstance().registerService(deviceName!!)
    }

    private fun stopService() {
        LanLinkReceiver.getInstance().unregisterService()
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
        initLanLinkReceiver()
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
        initLanLinkReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        LanLinkReceiver.getInstance().destroy()
        Process.killProcess(Process.myPid())
        System.exit(0)
    }

    override fun onReceive(serviceInfo: ServiceInfo, type: String, data: Any, resultCode: Int) {
        super.onReceive(serviceInfo, type, data, resultCode)
        if (resultCode != RESULT_SUCCESS) {
            println("onMessage: resultCode=$resultCode")
            return
        }
        when (type) {
            TaskInfo::class.qualifiedName -> {
                if (data is TaskInfo) {
                    if (data.actionType == ActionType.cast) {
                        val media = data.media
                        val intent = Intent(this, CastViewActivity::class.java)
                        intent.putExtra(KEY_MEDIA_INFO, media)
                        startActivity(intent)
                    } else if (data.actionType == ActionType.store) {
                        val mediaItem = MediaItem(data.media)
                        startDownload(mediaItem)
                        mAdapter.add(mediaItem)
                    }
                }
            }
            "user-info" -> {
                if (data is User) {
                    println("onMessage: $data")
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
        if (file.parentFile?.exists() == false) {
            file.parentFile?.mkdirs()
        }

        val outputStream = FileOutputStream(file)
        val buffer = ByteArray(1024 * 128)
        var len: Int
        while (inputStream.read(buffer).also { len = it } != -1) {
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
                val item = mediaList[position]
                if (item.status == STATUS_DOWNLOADED) {
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