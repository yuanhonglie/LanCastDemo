package com.yhl.cast.client

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Message
import android.os.Process
import android.text.TextUtils
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yhl.lanlink.LanLink.Companion.getInstance
import com.yhl.lanlink.base.BaseActivity
import com.yhl.lanlink.data.TaskInfo
import kotlinx.android.synthetic.main.activity_receiver.*
import kotlinx.android.synthetic.main.layout_lv_header.*
import java.util.*

class ReceiverActivity : BaseActivity(), View.OnClickListener {
    private var castViewStarted = false
    private var paused = false
    private var mDeviceName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receiver)
        initView()
        //checkPermission()
    }

    private fun initCastServer() {}
    private fun initView() {
        ivBack.setOnClickListener { this }
        btnStop.setOnClickListener { this }
        btnStart.setOnClickListener(this)
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
            finish()
            return
        }
        if (grantResults.size <= 0) {
            finish()
            return
        }
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            finish()
            return
        }
        initCastServer()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Process.killProcess(Process.myPid())
        System.exit(0)
    }

    override fun finish() {
        super.finish()
    }

    override fun onPause() {
        super.onPause()
        paused = true
    }

    override fun onResume() {
        super.onResume()
        paused = false
        castViewStarted = false
    }

    override fun onMessage(msg: Message) {
        super.onMessage(msg)
        when (msg.what) {
            100 -> {
                val taskInfo = msg.obj
                println("onMessage: taskInfo = $taskInfo")
                if (taskInfo is TaskInfo) {
                    val media = taskInfo.media
                    //playMedia(media)
                    val intent = Intent(this, CastViewActivity::class.java)
                    intent.putExtra(KEY_MEDIA_INFO, media)
                    startActivity(intent)
                    castViewStarted = true
                }
            }
            101 -> {
                if (paused && castViewStarted) {
                    val intent = Intent(this, CastViewActivity::class.java)
                    intent.putExtra(KEY_CAST_EXIT, true)
                    startActivity(intent)
                }
            }
        }
    }

    companion object {
        private const val TAG = "ReceiverActivity"
        private const val SERVICE_NAME = "DIDA Handbag"
        private const val SUFFIX_LENGTH = 5
    }
}