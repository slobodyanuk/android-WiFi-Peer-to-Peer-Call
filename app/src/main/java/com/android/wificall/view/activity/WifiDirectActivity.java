package com.android.wificall.view.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.wificall.R;
import com.android.wificall.data.event.ActivityEvent;
import com.android.wificall.router.Configuration;
import com.android.wificall.router.broadcast.WifiDirectBroadcastReceiver;
import com.android.wificall.util.DeviceActionListener;
import com.android.wificall.view.fragment.DeviceDetailsFragment;
import com.android.wificall.view.fragment.DeviceListFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.OnClick;

public class WifiDirectActivity extends BaseActivity implements WifiP2pManager.ChannelListener, DeviceActionListener {

    public static boolean isGroupOwner = false;
    public static boolean isVisible = false;
    private final IntentFilter mIntentFilter = new IntentFilter();
    private final IntentFilter mWifiIntentFilter = new IntentFilter();
    private String TAG = WifiDirectActivity.this.getClass().getName();
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;
    private boolean isWifiConnected;
    private WifiP2pManager mWifiManager;
    private WifiP2pManager.Channel mWifiChannel;
    private WifiDirectBroadcastReceiver mReceiver = null;

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mReceiver = new WifiDirectBroadcastReceiver(mWifiManager, mWifiChannel, this);
        registerReceiver(mReceiver, mIntentFilter);
        isVisible = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initOwnerDialog();
        initRouterSettings();

    }

    public void connectToAccessPoint(String ssid, String passphrase) {

        Log.d(TAG, "Trying to connect to AP : (" + ssid + "," + passphrase + ")");

        WifiConfiguration wc = new WifiConfiguration();
        wc.SSID = "\"" + ssid + "\"";
        wc.preSharedKey = "\"" + passphrase + "\""; // "\""+passphrase+"\"";
        wc.status = WifiConfiguration.Status.ENABLED;
        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        // connect to and enable the connection
        WifiManager manager =  (WifiManager) getSystemService(Context.WIFI_SERVICE);;
        int netId = manager.addNetwork(wc);
        manager.enableNetwork(netId, true);
        manager.setWifiEnabled(true);

        Log.d(TAG, "Connected? ip = " + manager.getConnectionInfo().getIpAddress());
        Log.d(TAG, "Connected? bssid = " + manager.getConnectionInfo().getBSSID());
        Log.d(TAG, "Connected? ssid = " + manager.getConnectionInfo().getSSID());

        if (manager.getConnectionInfo().getIpAddress() != 0) {
            this.isWifiConnected = true;
            Toast.makeText(this, "Connected!!! ip = " + manager.getConnectionInfo().getIpAddress(),
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(
                    this,
                    "WiFi AP connection failed... ip = " + manager.getConnectionInfo().getIpAddress() + "(" + ssid
                            + "," + passphrase + ")", Toast.LENGTH_LONG).show();
        }

    }

    public void setWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    private void initOwnerDialog() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Creating group")
                .setMessage("Do you want to join the group as owner?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isGroupOwner = true;
                        mReceiver.createGroup();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void initRouterSettings() {
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mWifiManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mWifiChannel = mWifiManager.initialize(this, getMainLooper(), null);
    }

    public void resetData() {
        DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
        DeviceDetailsFragment fragmentDetails = (DeviceDetailsFragment) getFragmentManager().findFragmentById(
                R.id.frag_detail);
        if (fragmentList != null) {
            fragmentList.clearPeers();
        }
        if (fragmentDetails != null) {
            fragmentDetails.resetViews();
        }
    }

    @OnClick(R.id.btn_discover)
    public void onDiscoverClick() {
        if (!isWifiP2pEnabled) {
            Toast.makeText(this, R.string.p2p_off_warning, Toast.LENGTH_SHORT).show();
        }
        final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager().findFragmentById(
                R.id.frag_list);
        fragment.onInitiateDiscovery();
        mWifiManager.discoverPeers(mWifiChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Toast.makeText(WifiDirectActivity.this, "Discovery Initiated", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reasonCode) {
                Toast.makeText(WifiDirectActivity.this, "Discovery Failed : " + reasonCode, Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    @OnClick(R.id.btn_switch)
    public void onChatClick() {
        Intent i = new Intent(getApplicationContext(), MessageActivity.class);
        startActivity(i);
    }

    @OnClick(R.id.btn_call)
    public void onCallClick() {
        Intent i = new Intent(getApplicationContext(), CallActivity.class);
        startActivity(i);
    }

    @Override
    public void showDetails(WifiP2pDevice device) {
        DeviceDetailsFragment fragment = (DeviceDetailsFragment) getFragmentManager().findFragmentById(R.id.frag_detail);
        fragment.showDetails(device);
    }

    @Override
    public void cancelDisconnect() {
        if (mWifiManager != null) {
            final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager().findFragmentById(
                    R.id.frag_list);
            if (fragment.getDevice() == null || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE
                    || fragment.getDevice().status == WifiP2pDevice.INVITED) {

                mWifiManager.cancelConnect(mWifiChannel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(WifiDirectActivity.this, "Aborting connection", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WifiDirectActivity.this,
                                "Connect abort request failed. Reason Code: " + reasonCode, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ActivityEvent event) {
        Toast.makeText(this, event.getMsg(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void connect(WifiP2pConfig config) {
        mWifiManager.connect(mWifiChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(WifiDirectActivity.this, "Connect failed. Retry.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void disconnect() {
        final DeviceDetailsFragment fragment = (DeviceDetailsFragment) getFragmentManager().findFragmentById(
                R.id.frag_detail);
        fragment.resetViews();
        mWifiManager.removeGroup(mWifiChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);

            }

            @Override
            public void onSuccess() {
                fragment.getView().setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_wifi_direct;
    }

    @Override
    public void onChannelDisconnected() {
        if (mWifiManager != null && !retryChannel) {
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
            resetData();
            retryChannel = true;
            mWifiManager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(this, "Channel is probably lost permanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
        isVisible = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

}
