package com.yhl.http

import com.yhl.data.TaskInfo
import io.reactivex.Flowable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

const val URI_MEDIA_TRANSFER = "/media/transfer"

interface MediaServerApi {

    /**
     * 请求图片、视频投屏
     */
    @Headers("Content-Type:application/json")
    @POST("/media/cast")
    fun requestCastMedia(): Flowable<Response<ResponseBody>>

    /**
     * 请求文件传输
     */
    @Headers("Content-Type:application/json")
    @POST(URI_MEDIA_TRANSFER)
    fun requestTransfer(@Body task: TaskInfo): Flowable<Response<ResponseBody>>

    /**
     * 请求屏幕镜像
     */
    @Headers("Content-Type:application/json")
    @POST("/media/mirror")
    fun requestMirrorCast(): Flowable<Response<ResponseBody>>
}