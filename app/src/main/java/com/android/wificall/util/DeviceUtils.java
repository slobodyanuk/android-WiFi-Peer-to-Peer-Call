package com.android.wificall.util;

import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;

import java.util.Random;

/**
 * Created by matviy on 12.09.16.
 */
public class DeviceUtils {

    public static String getDeviceStatus(int deviceStatus) {
        Log.d("DeviceUtils", "Peer status :" + deviceStatus);
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";
        }
    }

    public static WifiP2pDevice getDummyDevice() {
        Random r = new Random();
        WifiP2pDevice device = new WifiP2pDevice();
        device.deviceName = "test" + r.nextInt(100);
        device.status = r.nextInt(4);
        return device;
    }

    public static WifiP2pConfig getConfig(WifiP2pDevice device) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.groupOwnerIntent = 0;
        config.wps.setup = WpsInfo.PBC;
        return config;
    }

}
