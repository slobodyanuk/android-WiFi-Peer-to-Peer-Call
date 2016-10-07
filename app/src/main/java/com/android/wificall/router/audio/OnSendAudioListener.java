package com.android.wificall.router.audio;

import android.net.wifi.p2p.WifiP2pManager;

/**
 * Created by Serhii Slobodyanuk on 22.09.2016.
 */
public interface OnSendAudioListener {

    void onSendAudioData(byte[] data);

    void onUpdateConnection(WifiP2pManager mWifiManager, WifiP2pManager.Channel mWifiChannel);

    void onCompleted();

}
