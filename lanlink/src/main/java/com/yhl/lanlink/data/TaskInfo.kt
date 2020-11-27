package com.yhl.lanlink.data

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

enum class MediaType {
    image, video, stream
}

enum class ActionType {
    cast, store
}

enum class PlayMode {
    default, loop
}