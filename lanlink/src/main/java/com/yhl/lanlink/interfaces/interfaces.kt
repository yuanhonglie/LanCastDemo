package com.yhl.lanlink.interfaces

import android.net.Uri
import com.yhl.lanlink.Msg
import com.yhl.lanlink.RESULT_FAILED
import com.yhl.lanlink.RESULT_SUCCESS
import com.yhl.lanlink.ServiceInfo
import com.yhl.lanlink.data.ActionType
import com.yhl.lanlink.data.MediaType

interface RegistrationListener {
    /**
     * 服务注册事件回调
     * @param resultCode [RESULT_SUCCESS] ：注册成功，
     * <br></br>[RESULT_FAILED] ：注册失败
     */
    fun onServiceRegistered(resultCode: Int)

    /**
     * 服务注销事件回调
     * @param resultCode [RESULT_SUCCESS] ：注册成功，
     * <br></br>[RESULT_FAILED] ：注册失败
     */
    fun onServiceUnregistered(resultCode: Int)
}

interface DiscoveryListener {
    /**
     * 开始发现服务事件回调
     * @param resultCode {@link RESULT_SUCCESS} ：注册成功，
     *                  <br>{@link RESULT_FAILED} ：注册失败
     */
    fun onDiscoveryStart(resultCode: Int)

    /**
     * 停止发现服务事件回调
     * @param resultCode {@link RESULT_SUCCESS} ：注册成功，
     *                  <br>{@link RESULT_FAILED} ：注册失败
     */
    fun onDiscoveryStop(resultCode: Int)

    /**
     * 发现服务回调
     * @param serviceInfo 被发现的服务信息，一个{@link ServiceInfo}实例
     */
    fun onServiceFound(serviceInfo: ServiceInfo)

    /**
     * 服务丢失回调
     * @param serviceInfo 丢失的服务信息，一个{@link ServiceInfo}实例
     */
    fun onServiceLost(serviceInfo: ServiceInfo)
}

interface ConnectionListener {
    /**
     * 连接服务事件
     */
    fun onConnect(serviceInfo: ServiceInfo, resultCode: Int)

    /**
     * 断开服务事件
     */
    fun onDisconnect(serviceInfo: ServiceInfo, resultCode: Int)
}


interface MessageListener {
    /**
     * 自定义消息事件
     */
    fun onReceive(serviceInfo: ServiceInfo, type: String, data: Any, resultCode: Int)
}

interface InitializeListener {
    /**
     * SDK初始化成功
     */
    fun onInitialized()
}

interface ILinkReceiver {

    fun isInitialized(): Boolean

    fun setInitializeListener(listener: InitializeListener?)

    fun setConnectionListener(listener: ConnectionListener?)

    fun setRegistrationListener(listener: RegistrationListener?)

    fun setMessageListener(listener: MessageListener?)

    fun registerService(name: String)

    fun unregisterService()

    fun registerMessageCodec(codec: MessageCodec<*>)

    fun sendMessage(serviceInfo: ServiceInfo, msg: Any, tag: String?)

    fun sendMessage(serviceInfo: ServiceInfo, msg: Any)

    fun destroy()
}

interface ILinkSender {

    fun isInitialized(): Boolean

    fun setInitializeListener(listener: InitializeListener?)

    fun setDiscoveryListener(listener: DiscoveryListener?)

    fun setConnectionListener(listener: ConnectionListener?)

    fun setMessageListener(listener: MessageListener?)

    fun startDiscovery()

    fun stopDiscovery()

    fun connect(serviceInfo: ServiceInfo)

    fun disconnect(serviceInfo: ServiceInfo)

    fun registerMessageCodec(codec: MessageCodec<*>)

    fun castMedia(serviceInfo: ServiceInfo, path: String, mediaType: MediaType)

    fun transferFile(serviceInfo: ServiceInfo, path: String)

    fun castExit(serviceInfo: ServiceInfo)

    fun sendMessage(serviceInfo: ServiceInfo, msg: Any, tag: String?)

    fun sendMessage(serviceInfo: ServiceInfo, msg: Any)

    fun serveFile(path: String?): String

    fun serveFile(uri: Uri?): String

    fun destroy()
}

abstract class MessageCodec<T> {

    abstract fun getMessageType() : String

    abstract  fun encode(msg: T): ByteArray

    abstract fun decode(data: ByteArray): T

    internal fun encodeInner(data: Any) = Msg(getMessageType(), encode(data as T))

    internal fun decodeInner(msg: Msg) = decode(msg.data)
}