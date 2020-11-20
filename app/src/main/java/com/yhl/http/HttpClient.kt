package com.yhl.http

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

private const val TIME_OUT = 15L

object HttpClient {
    var client: OkHttpClient
    init {
        val builder = OkHttpClient.Builder()
        builder.connectTimeout(TIME_OUT, TimeUnit.SECONDS)
            .writeTimeout(TIME_OUT, TimeUnit.SECONDS)
            .readTimeout(TIME_OUT, TimeUnit.SECONDS)
        client = builder.build()
    }


}