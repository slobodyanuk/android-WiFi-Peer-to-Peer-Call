package com.android.wificall;

import android.app.Activity;
import android.app.Application;
import android.content.ContextWrapper;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;

import com.android.wificall.view.activity.RoleActivity;
import com.android.wificall.view.activity.WifiDirectActivity;
import com.pixplicity.easyprefs.library.Prefs;

import java.lang.reflect.Method;

/**
 * Created by matviy on 28.07.16.
 */
public class App extends Application implements Application.ActivityLifecycleCallbacks {

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;

    @Override
    public void onCreate() {
        super.onCreate();

        new Prefs.Builder()
                .setContext(this)
                .setMode(ContextWrapper.MODE_PRIVATE)
                .setPrefsName(getPackageName())
                .setUseDefaultSharedPreference(true)
                .build();

        // Fabric.with(this, new Crashlytics());
        registerActivityLifecycleCallbacks(this);
    }

    public void setWifiManager(WifiP2pManager manager, WifiP2pManager.Channel channel) {
        this.mManager = manager;
        this.mChannel = channel;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (activity instanceof RoleActivity) {
            deletePersistentGroups();
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (activity instanceof WifiDirectActivity) {
                deletePersistentGroups();
        }
    }

    private void deletePersistentGroups() {
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
