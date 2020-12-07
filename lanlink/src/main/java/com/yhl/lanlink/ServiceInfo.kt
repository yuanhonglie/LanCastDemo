package com.yhl.lanlink

import android.os.Parcel
import android.os.Parcelable
import com.yhl.lanlink.channel.Channel
import com.yhl.lanlink.data.MediaType
import com.yhl.lanlink.util.md5

class ServiceInfo: Parcelable {

    @Transient
    internal var channel: Channel? = null

    var id: String = ""
        private set(value) {
            field = value
        }

    var name: String = ""
        private set(value) {
            field = value
        }
    var host: String = ""
        private set(value) {
            field = value
        }
    var port: Int = 0
        private set(value) {
            field = value
        }

    constructor(name: String?, host: String?, port: Int) {
        this.name = name ?: ""
        this.host = host ?: ""
        this.port = port
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
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(host)
        parcel.writeInt(port)
    }

    fun readFromParcel(parcel: Parcel) {
        name = parcel.readString() ?: ""
        host = parcel.readString() ?: ""
        port = parcel.readInt()
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
                .append(id)
        }
    }
}

fun ServiceInfo.sendCastTask(uri: String, type: MediaType) {
    channel?.sendCastTask(uri, type)
}

fun ServiceInfo.sendCastExit() {
    channel?.sendCastExit()
}

fun ServiceInfo.isConnected() = channel?.isConnected ?: false