package com.android.wificall.util;

import android.net.wifi.p2p.WifiP2pManager;

import java.lang.reflect.Method;

/**
 * Created by matviy on 15.09.16.
 */
public class WifiUtils {

    public static void deletePersistentGroups(WifiP2pManager mManager, WifiP2pManager.Channel mChannel) {
        if (mManager != null && mChannel != null) {
            try {
                Method[] methods = WifiP2pManager.class.getMethods();
                for (int i = 0; i < methods.length; i++) {
                    if (methods[i].getName().equals("deletePersistentGroup")) {
                        // Delete any persistent group
                        for (int netId = 0; netId < 32; netId++) {
                            methods[i].invoke(mManager, mChannel, netId, null);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
