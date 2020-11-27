package com.yhl.lanlink.server

import com.yhl.lanlink.FILE_SERVER_PORT
import com.yhl.lanlink.LOG_DISABLE
import fi.iki.elonen.SimpleWebServer
import java.io.File



class FileServer: SimpleWebServer(null,
    FILE_SERVER_PORT, File("."),
    LOG_DISABLE
) {

    private val TAG = FileServer::class.simpleName

    /*
    init {
        val mimeTypes: Array<String> = getMimeTypes()
        for (mime in mimeTypes) {
            val indexFiles: Array<String> = getIndexFilesForMimeType(mime)
            if (!LOG_DISABLE) {
                print("# Found plugin for Mime type: \"$mime\"")
                if (indexFiles != null) {
                    print(" (serving index files: ")
                    for (indexFile in indexFiles) {
                        print("$indexFile ")
                    }
                }
                println(").")
            }
            registerPluginForMimeType(
                indexFiles,
                mime,
                getWebServerPlugin(mime),
                null
            )
        }
    }


    override fun getMimeTypes(): Array<String> = arrayOf("video/mpeg", "video/quicktime", "image/bmp", "image/jpeg")

    override fun getIndexFilesForMimeType(mime: String?) = emptyArray<String>()

    override fun getWebServerPlugin(mimeType: String?): WebServerPlugin {
        when
    }

    val videoServerPlugin = object : WebServerPlugin {
        override fun canServeUri(uri: String?, rootDir: File?): Boolean {

        }

        override fun initialize(commandLineOptions: MutableMap<String, String>?) {

        }

        override fun serveFile(
            uri: String?,
            headers: MutableMap<String, String>?,
            session: IHTTPSession?,
            file: File?,
            mimeType: String?
        ): Response {
        }
    }*/

}