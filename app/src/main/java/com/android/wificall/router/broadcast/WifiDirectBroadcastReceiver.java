package com.android.wificall.router.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.widget.Toast;

import com.android.wificall.R;
import com.android.wificall.data.Client;
import com.android.wificall.router.Configuration;
import com.android.wificall.router.NetworkManager;
import com.android.wificall.router.Receiver;
import com.android.wificall.router.Sender;
import com.android.wificall.view.activity.WifiDirectActivity;
import com.android.wificall.view.fragment.DeviceDetailsFragment;
import com.android.wificall.view.fragment.DeviceListFragment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.List;

/**
 * Created by slobodyanuk on 11.07.16.
 */
public class WifiDirectBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = WifiDirectBroadcastReceiver.class.getCanonicalName();

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WifiDirectActivity mActivity;
    public static String MAC;
    private int state;

    public WifiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, WifiDirectActivity activity) {
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mActivity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

            // UI update to indicate wifi p2p status.
           state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state != WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                mActivity.setWifiP2pEnabled(false);
                mActivity.resetData();
            }else{
                mActivity.setWifiP2pEnabled(true);
            }

            Log.d(TAG, "P2PACTION : WIFI_P2P_STATE_CHANGED_ACTION state = " + state);
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            // request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            if (mManager != null) {
                mManager.requestPeers(mChannel,
                        (WifiP2pManager.PeerListListener) mActivity.getFragmentManager().findFragmentById(R.id.frag_list));
            }
            Log.d(TAG, "P2PACTION : WIFI_P2P_PEERS_CHANGED_ACTION");
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            if (mManager == null) {
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                // we are connected with the other device, request connection
                // info to find group owner IP
                DeviceDetailsFragment fragment = (DeviceDetailsFragment) mActivity.getFragmentManager().findFragmentById(
                        R.id.frag_detail);
                mManager.requestConnectionInfo(mChannel, fragment);
            } else {
                // It's a disconnect
                Log.d(TAG, "P2PACTION : WIFI_P2P_CONNECTION_CHANGED_ACTION -- DISCONNECT");
                mActivity.resetData();
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            DeviceListFragment fragment = (DeviceListFragment) mActivity.getFragmentManager().findFragmentById(
                    R.id.frag_list);
            fragment.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));

            MAC = ((WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)).deviceAddress;

            //Set yourself on connection
            NetworkManager.setSelf(new Client(((WifiP2pDevice) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)).deviceAddress, Configuration.GO_IP,
                    ((WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)).deviceName,
                    ((WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)).deviceAddress));

            //Launch receiver and sender once connected to someone
            if (!Receiver.running) {
                Receiver r = new Receiver(this.mActivity);
                new Thread(r).start();
                Sender s = new Sender();
                new Thread(s).start();
            }

            mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null) {
                        String passphrase = group.getPassphrase();

                        String ssid = group.getNetworkName();

                        Log.d(TAG, "GROUP INFO AVALABLE");
                        Log.d(TAG, " SSID : " + ssid + "\n Passphrase : " + passphrase);
                    }
                }
            });
        }
    }

    public void createGroup() {
        // Wifi Direct mode is enabled
        if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
            mActivity.setWifiP2pEnabled(true);
            mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    Log.d(TAG, "P2P Group created");

                }

                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "P2P Group failed " + reason);
                }
            });
        }
    }
}
