package com.android.wificall.data.event;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Created by matviy on 16.09.16.
 */
public class UpdateDeviceEvent {

    private WifiP2pDevice mDevice;

    public UpdateDeviceEvent(WifiP2pDevice mDevice) {
        this.mDevice = mDevice;
    }

    public WifiP2pDevice getDevice() {
        return mDevice;
    }

    public void setDevice(WifiP2pDevice mDevice) {
        this.mDevice = mDevice;
    }
}
