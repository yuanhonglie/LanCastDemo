package com.yhl.lanlink.interfaces

import android.os.Messenger
import com.yhl.lanlink.IConnectionListener
import com.yhl.lanlink.IDiscoveryListener
import com.yhl.lanlink.IRegistrationListener
import com.yhl.lanlink.ServiceInfo
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
    fun onDiscoveryStart(resultCode: Int);

    /**
     * 停止发现服务事件回调
     * @param resultCode {@link RESULT_SUCCESS} ：注册成功，
     *                  <br>{@link RESULT_FAILED} ：注册失败
     */
    fun onDiscoveryStop(resultCode: Int);

    /**
     * 发现服务回调
     * @param serviceInfo 被发现的服务信息，一个{@link ServiceInfo}实例
     */
    fun onServiceFound(serviceInfo: ServiceInfo);

    /**
     * 服务丢失回调
     * @param serviceInfo 丢失的服务信息，一个{@link ServiceInfo}实例
     */
    fun onServiceLost(serviceInfo: ServiceInfo);
}

interface ConnectionListener {
    /**
     * 连接服务事件
     */
    fun onConnect(serviceInfo: ServiceInfo, resultCode: Int)

    /**
     * 断开服务事件
     */
    fun onDisconnect(serviceInfo: ServiceInfo, resultCode: Int);
}

interface ILanLink {

    fun registerService(name: String)

    fun unregisterService()

    fun startDiscovery()

    fun stopDiscovery()

    fun connect(serviceInfo: ServiceInfo)

    fun disconnect(serviceInfo: ServiceInfo)

    fun setRegistrationListener(listener: RegistrationListener?)

    fun setDiscoveryListener(listener: DiscoveryListener?)

    fun setConnectionListener(listener: ConnectionListener?)

    fun setClientMessenger(messenger: Messenger)

    fun sendCastTask(serviceInfo: ServiceInfo, uri: String, type: MediaType)

    fun sendCastExit(serviceInfo: ServiceInfo)

    fun destroy()
}