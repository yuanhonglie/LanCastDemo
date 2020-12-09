package com.yhl.cast.client

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.*
import java.io.IOException

class DownloadProgressResponseBody(

    private val request: Request,
    private val responseBody: ResponseBody,
    private val progressListener: DownloadProgressListener
) : ResponseBody() {
    private val NOTIFY_SIZE = 1024 * 100L

    private var bufferedSource: BufferedSource? = null

    override fun contentLength() = responseBody.contentLength()

    override fun contentType() = responseBody.contentType()

    override fun source(): BufferedSource {
        if (bufferedSource == null) {
            bufferedSource = source(responseBody.source()).buffer()
        }
        return bufferedSource!!
    }

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            var totalBytesRead: Long = 0
            var lastNotifySize = 0
            var startTime = 0L
            @Throws(IOException::class)
            override fun read(sink: Buffer, byteCount: Long): Long {
                if (startTime == 0L) {
                    startTime = System.currentTimeMillis()
                }
                val bytesRead = super.read(sink, byteCount)
                // read() returns the number of bytes read, or -1 if this source is exhausted.
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                if ((totalBytesRead - lastNotifySize > NOTIFY_SIZE) or (bytesRead == -1L)) {
                    lastNotifySize = totalBytesRead.toInt()
                    progressListener?.update(
                        request.url.toString(),
                        totalBytesRead,
                        responseBody.contentLength(),
                        bytesRead == -1L,
                        System.currentTimeMillis() - startTime
                    )
                }

                return bytesRead
            }
        }
    }

}

interface DownloadProgressListener {
    fun update(
        url: String,
        bytesRead: Long,
        contentLength: Long,
        done: Boolean,
        duration: Long
    )
}

class DownloadProgressInterceptor(private val listener: DownloadProgressListener): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse = chain.proceed(chain.request())

        return originalResponse.newBuilder()
            .body(
                DownloadProgressResponseBody(
                    chain.request(),
                    originalResponse.body!!,
                    listener
                )
            )
            .build()
    }
}