package com.yhl.lanlink;

import com.yhl.lanlink.ServiceInfo;

interface IConnectionListener {
    /**
     * 连接服务事件
     */
    void onConnect(out ServiceInfo serviceInfo, int resultCode);

    /**
     * 断开服务事件
     */
    void onDisconnect(out ServiceInfo serviceInfo, int resultCode);
}