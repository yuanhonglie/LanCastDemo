package com.yhl.lanlink.server

import android.content.Context
import android.net.Uri
import com.yhl.lanlink.FILE_SERVER_PORT
import com.yhl.lanlink.LOG_DISABLE
import com.yhl.lanlink.util.md5
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.SimpleWebServer
import fi.iki.elonen.WebServerPlugin
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException


class FileServer(private val context: Context, private val mConnectionManager: ConnectionManager): SimpleWebServer(null,
    FILE_SERVER_PORT, File("."),
    LOG_DISABLE
) {

    private val TAG = FileServer::class.simpleName
    private val fileMap = mutableMapOf<String, String>()


    private val fileServerPlugin = object : WebServerPlugin {
        override fun canServeUri(uri: String?, rootDir: File?): Boolean {
            val key = parseFileKey(uri)
            println("canServeUri: uri=$uri, key=$key, rootDir=$rootDir?.")
            return fileMap.containsKey(key)
        }

        override fun initialize(commandLineOptions: MutableMap<String, String>?) {

        }

        override fun serveFile(
            uri: String?,
            headers: MutableMap<String, String>,
            session: IHTTPSession?,
            file: File,
            mimeType: String
        ): Response {
            return this@FileServer.serveFile(uri, headers, file, mimeType)
        }
    }

    private fun parseFileKey(uri: String?) = uri?.substringAfterLast('/')

    init {
        MIME_TYPES = mutableMapOf("jpg" to "image/jpeg", "png" to "image/jpeg", "jpeg" to "image/jpeg", "mp4" to "video/mpeg")
        val mimeTypes = arrayOf("video/mpeg", "video/quicktime", "image/bmp", "image/jpeg", "application/octet-stream")
        for (mime in mimeTypes) {
            val indexFiles = emptyArray<String>()
            registerPluginForMimeType(
                indexFiles,
                mime,
                fileServerPlugin,
                null
            )
        }
    }

    fun serveFile(path: String): String {
        val md5 = path.md5()
        fileMap[md5] = path
        return md5
    }

    fun serveFile(
        uri: String?,
        header: MutableMap<String, String>,
        file: File,
        mime: String
    ): Response {
        val key = parseFileKey(uri)
        val path = fileMap[key]
        println("serveFile: path=$path, key=$key, uri=$uri")
        val file = File(path)

        var res: Response
        try {
            // Calculate etag
            val etag = Integer.toHexString((file.absolutePath + file.lastModified() + "" + file.length()).hashCode())

            // Support (simple) skipping:
            var startFrom: Long = 0
            var endAt: Long = -1
            val range = header["range"]
            if (range != null) {
                if (range.startsWith("bytes=")) {
                    val range = range.substring("bytes=".length)
                    val minus = range.indexOf('-')
                    try {
                        if (minus > 0) {
                            startFrom = range.substring(0, minus).toLong()
                            endAt = range.substring(minus + 1).toLong()
                        }
                    } catch (ignored: NumberFormatException) {
                    }
                }
            }

            // get if-range header. If present, it must match etag or else we
            // should ignore the range request
            val ifRange = header["if-range"]
            val headerIfRangeMissingOrMatching = ifRange == null || etag == ifRange
            val ifNoneMatch = header["if-none-match"]
            val headerIfNoneMatchPresentAndMatching =
                ifNoneMatch != null && ("*" == ifNoneMatch || ifNoneMatch == etag)

            // Change return code and add Content-Range header when skipping is
            // requested
            val fileLen = file.length()
            if (headerIfRangeMissingOrMatching && range != null && startFrom >= 0 && startFrom < fileLen) {
                // range request that matches current etag
                // and the startFrom of the range is satisfiable
                if (headerIfNoneMatchPresentAndMatching) {
                    // range request that matches current etag
                    // and the startFrom of the range is satisfiable
                    // would return range from file
                    // respond with not-modified
                    res = newFixedLengthResponse(
                        Response.Status.NOT_MODIFIED,
                        mime,
                        ""
                    )
                    res.addHeader("ETag", etag)
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1
                    }
                    var newLen = endAt - startFrom + 1
                    if (newLen < 0) {
                        newLen = 0
                    }
                    val fis = FileInputStream(file)
                    fis.skip(startFrom)
                    res = NanoHTTPD.newFixedLengthResponse(
                        Response.Status.PARTIAL_CONTENT,
                        mime,
                        fis,
                        newLen
                    )
                    res.addHeader("Accept-Ranges", "bytes")
                    res.addHeader("Content-Length", "" + newLen)
                    res.addHeader(
                        "Content-Range",
                        "bytes $startFrom-$endAt/$fileLen"
                    )
                    res.addHeader("ETag", etag)
                }
            } else {
                if (headerIfRangeMissingOrMatching && range != null && startFrom >= fileLen) {
                    // return the size of the file
                    // 4xx responses are not trumped by if-none-match
                    res = newFixedLengthResponse(
                        Response.Status.RANGE_NOT_SATISFIABLE,
                        NanoHTTPD.MIME_PLAINTEXT,
                        ""
                    )
                    res.addHeader("Content-Range", "bytes */$fileLen")
                    res.addHeader("ETag", etag)
                } else if (range == null && headerIfNoneMatchPresentAndMatching) {
                    // full-file-fetch request
                    // would return entire file
                    // respond with not-modified
                    res = newFixedLengthResponse(
                        Response.Status.NOT_MODIFIED,
                        mime,
                        ""
                    )
                    res.addHeader("ETag", etag)
                } else if (!headerIfRangeMissingOrMatching && headerIfNoneMatchPresentAndMatching) {
                    // range request that doesn't match current etag
                    // would return entire (different) file
                    // respond with not-modified
                    res = newFixedLengthResponse(
                        Response.Status.NOT_MODIFIED,
                        mime,
                        ""
                    )
                    res.addHeader("ETag", etag)
                } else {
                    // supply the file
                    res = newFixedFileResponse(file, mime)
                    res.addHeader("Content-Length", "" + fileLen)
                    res.addHeader("ETag", etag)
                }
            }
        } catch (ioe: IOException) {
            res = getForbiddenResponse("Reading file failed.")
        }

        return res
    }

    @Throws(FileNotFoundException::class)
    private fun newFixedFileResponse(
        file: File,
        mime: String
    ): Response {
        val res = NanoHTTPD.newFixedLengthResponse(
            Response.Status.OK,
            mime,
            FileInputStream(file),
            file.length()
        )
        res.addHeader("Accept-Ranges", "bytes")
        return res
    }
}