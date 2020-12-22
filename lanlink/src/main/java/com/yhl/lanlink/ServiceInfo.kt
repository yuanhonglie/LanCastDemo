package com.yhl.lanlink

import android.os.Parcel
import android.os.Parcelable
import com.yhl.lanlink.channel.Channel
import com.yhl.lanlink.log.Logger
import com.yhl.lanlink.util.md5

open class ServiceInfo: Parcelable {

    var id: String = ""
        private set

    var name: String = ""
        private set

    var host: String = ""
        private set

    var port: Int = 0
        private set

    @Transient
    internal var channel: Channel? = null

    @Transient
    var isConnected: Boolean = false
        private set
        get() = channel?.isConnected ?: false

    constructor(name: String?, host: String?, port: Int, connected: Boolean = false) {
        this.name = name ?: ""
        this.host = host ?: ""
        this.port = port
        this.isConnected = connected
        updateId()
    }

    private fun updateId() {
        id = if (host.isNotEmpty()) {
            "$name-$host-$port".md5()
        } else {
            ""
        }
    }

    constructor(): this("", "", 0)

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readInt(),
        parcel.readInt() != 0
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(host)
        parcel.writeInt(port)
        parcel.writeInt(if (isConnected) 1 else 0)
    }

    fun readFromParcel(parcel: Parcel) {
        name = parcel.readString() ?: ""
        host = parcel.readString() ?: ""
        port = parcel.readInt()
        isConnected = parcel.readInt() != 0
        updateId()
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ServiceInfo> {
        override fun createFromParcel(parcel: Parcel): ServiceInfo {
            return ServiceInfo(parcel)
        }

        override fun newArray(size: Int): Array<ServiceInfo?> {
            return arrayOfNulls(size)
        }
    }

    override fun toString(): String {
        return buildString {
            append(name).append(", ")
                .append(host).append(", ")
                .append(port).append(", ")
                .append(id).append(", ")
                .append(isConnected)
        }
    }
}

class ClientInfo(name: String?, host: String?, port: Int, val token: String): ServiceInfo(name, host, port)

fun ServiceInfo.sendMessage(msg: Msg) {
    Logger.i("ServiceInfo", "sendMessage: $channel, $msg")
    channel?.sendMessage(msg)
}