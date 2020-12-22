package com.yhl.lanlink

import android.os.Parcel
import android.os.Parcelable

class Msg : Parcelable {

    var tag: String = ""
        private set

    var data: ByteArray = ByteArray(0)
        private set

    constructor(tag: String, data: ByteArray = ByteArray(0)) {
        this.tag = tag
        this.data = data
    }

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.createByteArray() ?: ByteArray(0)
    )

    constructor() : this("")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Msg

        if (tag != other.tag) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tag.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(tag)
        parcel.writeByteArray(data)
    }

    fun readFromParcel(parcel: Parcel) {
        tag = parcel.readString() ?: ""
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