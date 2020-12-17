package com.yhl.lanlink;

import com.yhl.lanlink.ServiceInfo;
import com.yhl.lanlink.Msg;

interface IConnectionListener {
    /**
     * 连接服务事件
     */
    void onConnect(inout ServiceInfo serviceInfo, int resultCode);

    /**
     * 断开服务事件
     */
    void onDisconnect(inout ServiceInfo serviceInfo, int resultCode);

    /**
     * 消息接收事件
     */
    void onMessageReceive(inout ServiceInfo serviceInfo, inout Msg msg);

    /**
     * 消息发送事件
     */
    //void onMessageSend(inout ServiceInfo serviceInfo, int resultCode);
}