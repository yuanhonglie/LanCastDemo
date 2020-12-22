package com.yhl.lanlink

import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import com.yhl.lanlink.ipc.MemoryFileHelper

class Msg : Parcelable {

    var tag: String = ""
        private set

    var data: ByteArray = ByteArray(0)
        private set
        get() {
            val parcelFileDescriptor = pfd
            if (field.isEmpty() && parcelFileDescriptor != null) {
                val memoryFile = MemoryFileHelper.openMemoryFile(parcelFileDescriptor, size, MemoryFileHelper.OPEN_READONLY)
                field = ByteArray(size)
                memoryFile.readBytes(field, 0, 0, size)
                memoryFile.close()
            }
            return field
        }

    var pfd: ParcelFileDescriptor? = null

    var size: Int = 0

    constructor(tag: String, pfd: ParcelFileDescriptor? = null, size: Int = 0,  data: ByteArray = ByteArray(0)) {
        this.tag = tag
        this.data = data
        this.pfd = pfd
        this.size = size
    }

    constructor(tag: String, data: ByteArray = ByteArray(0)) : this(tag, null, 0, data)

    constructor(parcel: Parcel) : this(
            parcel.readString() ?: "",
            parcel.readFileDescriptor(),
            parcel.readInt(),
            parcel.createByteArray() ?: ByteArray(0)
    )

    //constructor() : this("")

    init {
        if (pfd == null && size == 0 && data.isNotEmpty()) {
            val memoryFile = MemoryFileHelper.createMemoryFile(tag, data.size)
            pfd = MemoryFileHelper.getParcelFileDescriptor(memoryFile)
            memoryFile.writeBytes(data, 0, 0, data.size)
            size = data.size
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(tag)
        val fd = pfd?.fileDescriptor
        parcel.writeFileDescriptor(fd)
        parcel.writeInt(size)
        parcel.writeByteArray(if (fd != null && size > 0) ByteArray(0) else data)
    }

    fun readFromParcel(parcel: Parcel) {
        tag = parcel.readString() ?: ""
        pfd = parcel.readFileDescriptor()
        size = parcel.readInt()
        data = parcel.createByteArray() ?: ByteArray(0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Msg> {
        override fun createFromParcel(parcel: Parcel): Msg {
            return Msg(parcel)
        }

        override fun newArray(size: Int): Array<Msg?> {
            return arrayOfNulls(size)
        }
    }
}