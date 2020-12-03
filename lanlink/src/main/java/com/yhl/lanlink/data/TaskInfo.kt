package com.yhl.lanlink.data

import com.yhl.lanlink.channel.Channel
import com.yhl.lanlink.util.md5
import java.net.InetAddress

data class Media(var uri: String, var mediaType: MediaType, var name: String = "") {
    var mimeType: String = ""
    var md5: String = ""
    var size: Long = 0
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

data class ServiceInfo(val name: String?, val host: InetAddress, val port: Int) {

    @Transient
    var channel: Channel? = null
    constructor(name: String?, host: String, port: Int): this(name, InetAddress.getByName(host), port)

    val id: String
    init {
        val address = host.hostAddress
        id = "$name-$address-$port".md5()
    }
}

data class ResultData<T>(val errorCode: Int, val errorMessage: String, val timestamp: Long, var data: T? = null)