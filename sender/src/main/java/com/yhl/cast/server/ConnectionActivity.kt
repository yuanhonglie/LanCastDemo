package com.yhl.cast.server

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.Process
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import com.yhl.cast.server.data.Hello
import com.yhl.lanlink.LanLinkSender
import com.yhl.lanlink.ServiceInfo
import com.yhl.lanlink.base.BaseActivity
import com.yhl.lanlink.interfaces.ConnectionListener
import com.yhl.lanlink.interfaces.DiscoveryListener
import com.yhl.lanlink.interfaces.InitializeListener
import com.yhl.lanlink.interfaces.MessageListener
import com.yhl.lanlink.log.Logger
import kotlinx.android.synthetic.main.activity_connection.*
import kotlinx.android.synthetic.main.layout_lv_footer.*
import kotlinx.android.synthetic.main.layout_lv_header.*

const val TAG = "ConnectionActivity"
const val SERVICE_INFO = "SERVICE_INFO"
const val MEDIA_TYPE = "MEDIA_TYPE"
private const val MSG_SEARCH_RESULT = 100
private const val MSG_CONNECT_FAILURE = 101
private const val MSG_CONNECT_SUCCESS = 104
private const val MSG_DISCONNECT = 103

class ConnectionActivity : BaseActivity() {

    lateinit var adapter: DeviceAdapter
    lateinit var mUiHandler: UIHandler
    var mSelectInfo: ServiceInfo? = null
    var isConnected = false
    var isSearching = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)
        initView()
        initLanlink()
        ivBack.setOnClickListener { onBackPressed() }
        tvVersion.setText("Demo V1.0")
    }

    private fun initLanlink() {
        mUiHandler = UIHandler()

        //sdk初始化
        LanLinkSender.getInstance().setConnectionListener(object : ConnectionListener {
            override fun onConnect(serviceInfo: ServiceInfo, resultCode: Int) {
                Toast.makeText(
                    this@ConnectionActivity,
                    "已连接 $serviceInfo",
                    Toast.LENGTH_LONG
                ).show()
                onServiceConnected(serviceInfo)
            }

            override fun onDisconnect(serviceInfo: ServiceInfo, resultCode: Int) {
                Toast.makeText(
                    this@ConnectionActivity,
                    "$serviceInfo 已断开",
                    Toast.LENGTH_LONG
                ).show()
            }
        })

        LanLinkSender.getInstance().setDiscoveryListener(object : DiscoveryListener {
            override fun onDiscoveryStart(resultCode: Int) {
                Toast.makeText(this@ConnectionActivity, "开始扫描服务", Toast.LENGTH_LONG).show()
            }

            override fun onDiscoveryStop(resultCode: Int) {
                Toast.makeText(this@ConnectionActivity, "停止扫描服务", Toast.LENGTH_LONG).show()
            }

            override fun onServiceFound(serviceInfo: ServiceInfo) {
                println("onServiceFound: $serviceInfo")
                onServiceDiscover(serviceInfo)
            }

            override fun onServiceLost(serviceInfo: ServiceInfo) {
                adapter.remove(serviceInfo)
            }
        })

        LanLinkSender.getInstance().setInitializeListener(object : InitializeListener {
            override fun onInitialized() {
                LanLinkSender.getInstance().startDiscovery()
                isSearching = true
            }
        })
    }

    override fun onReceive(serviceInfo: ServiceInfo, type: String, data: Any, resultCode: Int) {
        super.onReceive(serviceInfo, type, data, resultCode)
        when (type) {
            "hello-msg" -> {
                if (data is Hello) {
                    Logger.i(TAG, "message = $data");
                }
            }
        }
    }

    private fun initView() {
        btnConnect.setOnClickListener {
            if (mSelectInfo != null) {
                if (mSelectInfo?.id == adapter.selected) {
                    startCastMediaActivity()
                } else {
                    val serviceInfo = adapter.getSelectedDevice()
                    if (serviceInfo != null) {
                        LanLinkSender.getInstance().disconnect(serviceInfo)
                        LanLinkSender.getInstance().connect(serviceInfo)
                    }
                }
            } else {
                val serviceInfo = adapter.getSelectedDevice()
                if (serviceInfo != null) {
                    LanLinkSender.getInstance().connect(serviceInfo)
                }
            }
        }

        button2.setOnClickListener {
            val serviceInfo = mSelectInfo
            if (serviceInfo != null) {
                LanLinkSender.getInstance().disconnect(serviceInfo)
                isConnected = false
            }
        }

        //button2.visibility = View.INVISIBLE
        adapter = DeviceAdapter(this)
        adapter.onItemClickListener = object : OnItemClickListener {
            override fun onClick(position: Int, data: String) {
                updateButtonState(data)
            }
        }
        lvDevices.adapter = adapter
    }

    private fun updateButtonState(ip: String?) {
        if (isConnected) {
            btnConnect.isSelected = (mSelectInfo?.id ?: "") == ip
            btnConnect.isEnabled = true
        } else {
            btnConnect.isEnabled = true
            btnConnect.isSelected = false
        }
    }

    override fun onResume() {
        super.onResume()
        //updateButtonState(adapter.selected)
        refreshDeviceList()
        Log.i(TAG, "onResume: ")
        if (LanLinkSender.getInstance().isInitialized()) {
            LanLinkSender.getInstance().startDiscovery()
            isSearching = true
        }
    }

    override fun onPause() {
        super.onPause()
        LanLinkSender.getInstance().stopDiscovery()
        isSearching = false
    }

    private fun refreshDeviceList() {
        adapter.clear()
    }

    private fun startCastMediaActivity() {
        if (mSelectInfo != null) {
            Log.i(TAG, "startCastMediaActivity: service = " + System.identityHashCode(mSelectInfo))
            val intent = Intent()
            intent.setClass(this, CastMediaActivity::class.java)
            intent.putExtra(SERVICE_INFO, mSelectInfo)
            startActivity(intent)
        }
    }

    private fun startAlbumPickActivity() {
        Log.i(TAG, "startAlbumPickActivity: service = " + mSelectInfo?.name)
        val intent = Intent()
        intent.setClass(this, AlbumPickActivity::class.java)
        intent.putExtra(SERVICE_INFO, mSelectInfo)
        startActivity(intent)
    }


    inner class UIHandler : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_SEARCH_RESULT -> if (msg.obj != null) {
                    this@ConnectionActivity.onServiceDiscover(msg.obj as ServiceInfo)
                }
                MSG_CONNECT_SUCCESS -> if (msg.obj != null) {
                    this@ConnectionActivity.onServiceConnected(msg.obj as ServiceInfo)
                }
                MSG_CONNECT_FAILURE -> if (msg.obj != null) {
                    btnConnect.isEnabled = true
                    Toast.makeText(this@ConnectionActivity, msg.obj.toString(), Toast.LENGTH_SHORT)
                        .show()
                }
                MSG_DISCONNECT -> if (msg.obj != null) {
                    val serviceInfo = msg.obj as ServiceInfo
                    this@ConnectionActivity.onServiceDisconnected(serviceInfo)
                    Toast.makeText(
                        this@ConnectionActivity,
                        serviceInfo.name + "连接断开",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            super.handleMessage(msg)
        }
    }

    private fun onServiceConnected(serviceInfo: ServiceInfo) {
        //val type = if (msg.arg1 == IConnectListener.TYPE_LELINK) "Lelink" else if (msg.arg1 == IConnectListener.TYPE_DLNA) "DLNA" else if (msg.arg1 == IConnectListener.TYPE_NEW_LELINK) "NEW_LELINK" else "IM"
        mSelectInfo = serviceInfo
        isConnected = true
        Toast.makeText(this, serviceInfo.name + "连接成功", Toast.LENGTH_SHORT).show()
        startCastMediaActivity()
        updateButtonState(adapter.selected)
    }

    private fun onServiceDisconnected(serviceInfo: ServiceInfo) {
        if (mSelectInfo?.id == serviceInfo.id) {
            mSelectInfo = null
            adapter.selected = null
            isConnected = false
        }
        updateButtonState(adapter.selected)
    }

    private fun onServiceDiscover(serviceInfo: ServiceInfo) {
        adapter.add(serviceInfo)
        if (!adapter.devices.isEmpty() && adapter.selected == null) {
            adapter.selected = adapter.devices[0].id
            adapter.notifyDataSetChanged()
            updateButtonState(adapter.selected)
        }

        if (adapter.devices.isEmpty()) {
            //btnConnect.isSelected = false
            //adapter.selected = null
            //disconnectDevice()
        }
    }

    class DeviceAdapter(val context: Context) : BaseAdapter() {

        var devices = mutableListOf<ServiceInfo>()
        var devMap = mutableMapOf<String, ServiceInfo>()
        var onItemClickListener: OnItemClickListener? = null

        var selected: String? = null

        fun add(device: ServiceInfo) {
            if (!devMap.contains(device.id)) {
                Log.i(TAG, "add: $device\nip = ${device.id}\nsize = ${devices.size}")
                devices.add(device)
                notifyDataSetChanged()
                devMap[device.id] = device
            }
        }

        fun remove(device: ServiceInfo) {
            devices.remove(device)
            devMap.remove(device.id)
            notifyDataSetChanged()
        }

        fun clear() {
            devices.clear()
            devMap.clear()
            notifyDataSetChanged()
        }

        fun getSelectedDevice(): ServiceInfo? {
            val key = selected
            return if (key != null) devMap[key] else null
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view: View?
            val viewHolder: ViewHolder
            if (convertView == null) {
                view = LayoutInflater.from(context).inflate(R.layout.layout_device_item, null)
                viewHolder = ViewHolder(view)
                view?.tag = viewHolder
            } else {
                view = convertView
                viewHolder = view.tag as ViewHolder
            }

            //viewHolder.root?.setTag(position)
            viewHolder.device?.text = devices[position].name
            viewHolder.device?.isSelected = devices[position].id == selected
            //viewHolder.root?.setBackgroundColor(context.resources.getColor(if (devices[position] == selected) R.color.gray_e5e5e5 else R.color.transparent))
            viewHolder.root?.setOnClickListener {
                selected = devices[position].id
                onItemClickListener?.onClick(position, devices[position].id)
                notifyDataSetChanged()
            }

            return view as View
        }

        override fun getItem(position: Int): Any {
            return devices[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return devices.size
        }

        class ViewHolder(view: View?) {
            var root: View? = null
            var device: TextView? = null

            init {
                root = view?.findViewById(R.id.layoutWrapper)
                device = view?.findViewById(R.id.tvDevice)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectDevice()
        LanLinkSender.getInstance().destroy()
        Process.killProcess(Process.myPid())
        System.exit(0)
    }

    private fun disconnectDevice() {
        val serviceInfo = mSelectInfo
        if (serviceInfo != null) {
            LanLinkSender.getInstance().disconnect(serviceInfo)
            mSelectInfo = null
            isConnected = false
        }
    }

    interface OnItemClickListener {
        fun onClick(position: Int, data: String)
    }
}