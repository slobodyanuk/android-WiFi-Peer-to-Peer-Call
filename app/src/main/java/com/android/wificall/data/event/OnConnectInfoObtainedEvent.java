package com.android.wificall.data.event;

import android.net.wifi.p2p.WifiP2pInfo;

/**
 * Created by matviy on 16.09.16.
 */
public class OnConnectInfoObtainedEvent {

    private WifiP2pInfo info;

    public OnConnectInfoObtainedEvent(WifiP2pInfo info) {
        this.info = info;
    }

    public WifiP2pInfo getInfo() {
        return info;
    }

    public void setInfo(WifiP2pInfo info) {
        this.info = info;
    }
}
