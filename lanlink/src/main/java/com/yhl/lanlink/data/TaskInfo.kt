package com.yhl.lanlink.data

import android.os.Parcel
import android.os.Parcelable
import com.yhl.lanlink.channel.Channel
import com.yhl.lanlink.util.md5
import java.net.InetAddress

data class Media(var uri: String, var mediaType: MediaType, var name: String = ""): Parcelable {
    var mimeType: String = ""
    var md5: String = ""
    var size: Long = 0

    constructor(uri: String, mediaType: String, name: String): this(uri, MediaType.valueOf(mediaType), name)

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    ) {
        mimeType = parcel.readString() ?: ""
        md5 = parcel.readString() ?: ""
        size = parcel.readLong()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(uri)
        parcel.writeString(mediaType.toString())
        parcel.writeString(name)
        parcel.writeString(mimeType)
        parcel.writeString(md5)
        parcel.writeLong(size)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Media> {
        override fun createFromParcel(parcel: Parcel): Media {
            return Media(parcel)
        }

        override fun newArray(size: Int): Array<Media?> {
            return arrayOfNulls(size)
        }
    }
}

data class TaskInfo(var media: Media, var actionType: ActionType = ActionType.cast, var playMode: PlayMode = PlayMode.default, var albumName: String = "") {

    /*
    var list: MutableList<Media> = mutableListOf()
    constructor(list: MutableList<Media>, actionType: ActionType): this(actionType) {
        this.list = list
    }

    fun addMedia(media: Media) {
        list.add(media)
    }

    fun clearMediaList() {
        list.clear()
    }*/
}

enum class MediaType(val value: Int) {
    image(0), video(1), stream(2)
}

enum class ActionType(val value: Int) {
    cast(0), store(1)
}

enum class PlayMode(val value: Int) {
    default(0), loop(1)
}

data class ResultData<T>(val errorCode: Int, val errorMessage: String, val timestamp: Long, var data: T? = null)