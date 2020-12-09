package com.yhl.lanlink;

import com.yhl.lanlink.ServiceInfo;
import com.yhl.lanlink.Msg;
import com.yhl.lanlink.IConnectionListener;
import com.yhl.lanlink.IDiscoveryListener;
import com.yhl.lanlink.IRegistrationListener;

interface ILanLinkService {

    void registerService(String name);

    void unregisterService();

    void startDiscovery();

    void stopDiscovery();

    void connect(String serviceId);

    void disconnect(String serviceId);

    void setRegistrationListener(IRegistrationListener listener);

    void setDiscoveryListener(IDiscoveryListener listener);

    void setConnectionListener(IConnectionListener listener);

    void setClientMessenger(in Messenger messenger);

    void sendCastTask(String serviceId, String uri, String mediaType, String actionType);

    void sendCastExit(String serviceId);

    void send(String serviceId, inout Msg msg);

    void destroy();
}