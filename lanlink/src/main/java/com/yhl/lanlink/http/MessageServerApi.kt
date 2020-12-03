package com.yhl.lanlink.http

import com.yhl.lanlink.URI_CONNECTION_CONNECT
import com.yhl.lanlink.URI_CONNECTION_DISCONNECT
import com.yhl.lanlink.URI_CONNECTION_HEART_BEAT
import com.yhl.lanlink.URI_MEDIA_TRANSFER
import com.yhl.lanlink.data.ResultData
import com.yhl.lanlink.data.ServiceInfo
import com.yhl.lanlink.data.TaskInfo
import io.reactivex.Flowable
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface MediaServerApi {

    /**
     * 请求建立连接
     */
    @Headers("Content-Type:application/json")
    @POST(URI_CONNECTION_CONNECT)
    fun requestConnection(@Body serviceInfo: ServiceInfo): Call<ResultData<String>>

    /**
     * 心跳
     */
    @Headers("Content-Type:application/json")
    @POST(URI_CONNECTION_HEART_BEAT)
    fun requestHeartBeat(@Header("token") token: String, @Header("timestamp")timestamp: Long): Call<ResultData<Long>>

    /**
     * 请求断开连接
     */
    @Headers("Content-Type:application/json")
    @POST(URI_CONNECTION_DISCONNECT)
    fun requestDisconnection(@Header("token") token: String): Call<ResultData<String>>

    /**
     * 请求文件传输
     */
    @Headers("Content-Type:application/json")
    @POST(URI_MEDIA_TRANSFER)
    fun requestTransfer(@Header("token") token: String, @Body task: TaskInfo): Call<ResultData<String>>

    /**
     * 请求屏幕镜像
     */
    @Headers("Content-Type:application/json")
    @POST("/media/mirror")
    fun requestMirrorCast(): Flowable<Response<ResponseBody>>
}