package com.yhl.lanlink;

import com.yhl.lanlink.ServiceInfo;

interface IDiscoveryListener {
    /**
     * 开始发现服务事件回调
     * @param resultCode {@link RESULT_SUCCESS} ：注册成功，
     *                  <br>{@link RESULT_FAILED} ：注册失败
     */
    void onDiscoveryStart(int resultCode);

    /**
     * 停止发现服务事件回调
     * @param resultCode {@link RESULT_SUCCESS} ：注册成功，
     *                  <br>{@link RESULT_FAILED} ：注册失败
     */
    void onDiscoveryStop(int resultCode);

    /**
     * 发现服务回调
     * @param serviceInfo 被发现的服务信息，一个{@link ServiceInfo}实例
     */
    void onServiceFound(inout ServiceInfo serviceInfo);

    /**
     * 服务丢失回调
     * @param serviceInfo 丢失的服务信息，一个{@link ServiceInfo}实例
     */
    void onServiceLost(out ServiceInfo serviceInfo);
}