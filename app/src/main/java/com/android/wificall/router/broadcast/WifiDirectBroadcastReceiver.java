package com.android.wificall.router.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import com.android.wificall.data.Client;
import com.android.wificall.data.event.DisconnectEvent;
import com.android.wificall.data.event.GroupOwnerEvent;
import com.android.wificall.data.event.OnConnectInfoObtainedEvent;
import com.android.wificall.data.event.RequestPeersEvent;
import com.android.wificall.data.event.UpdateDeviceEvent;
import com.android.wificall.data.event.WifiP2PStateChangedEvent;
import com.android.wificall.router.Configuration;
import com.android.wificall.router.NetworkManager;
import com.android.wificall.router.Receiver;
import com.android.wificall.router.Sender;
import com.android.wificall.util.ConnectionInfoState;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by slobodyanuk on 11.07.16.
 */
public class WifiDirectBroadcastReceiver extends BroadcastReceiver implements WifiP2pManager.ConnectionInfoListener {

    private static final String TAG = WifiDirectBroadcastReceiver.class.getCanonicalName();

    public static boolean groupOwner = false;

    private ConnectionInfoState mConnectionInfoState = ConnectionInfoState.UNAVAILABLE;
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    public static String MAC;
    private int state;
    private boolean isGroupCreated = false;

    public WifiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel) {
        super();
        this.mManager = manager;
        this.mChannel = channel;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

            // UI update to indicate wifi p2p status.
            state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            EventBus.getDefault().post(new WifiP2PStateChangedEvent(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED));
            Log.d(TAG, "P2PACTION : WIFI_P2P_STATE_CHANGED_ACTION state = " + state);
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            EventBus.getDefault().post(new RequestPeersEvent());
            Log.d(TAG, "P2PACTION : WIFI_P2P_PEERS_CHANGED_ACTION");
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            if (mManager == null) {
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                // we are connected with the other device, request connection
                // info to find group owner IP
                mManager.requestConnectionInfo(mChannel, this);
            } else {
                // It's a disconnect
                Log.d(TAG, "P2PACTION : WIFI_P2P_CONNECTION_CHANGED_ACTION -- DISCONNECT");
                EventBus.getDefault().post(new DisconnectEvent());
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            EventBus.getDefault().post(new UpdateDeviceEvent((WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)));

            MAC = ((WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)).deviceAddress;

            //Set yourself on connection
            NetworkManager.setSelf(new Client(((WifiP2pDevice) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)).deviceAddress, Configuration.GO_IP,
                    ((WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)).deviceName,
                    ((WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)).deviceAddress));

            //Launch receiver and sender once connected to someone
            if (!Receiver.running) {
                Receiver r = new Receiver();
                new Thread(r).start();
                Sender s = new Sender();
                new Thread(s).start();
            }

            mManager.requestGroupInfo(mChannel, group -> {
                if (group != null) {
                    String passphrase = group.getPassphrase();
                    String ssid = group.getNetworkName();
                    Log.d(TAG, "GROUP INFO AVALABLE");
                    Log.d(TAG, " SSID : " + ssid + "\n Passphrase : " + passphrase);
                }
            });
        }
    }

    public void createGroup() {
        // Wifi Direct mode is enabled
        if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
            EventBus.getDefault().post(new WifiP2PStateChangedEvent(true));
            mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    Log.d(TAG, "P2P Group created");
                    isGroupCreated = true;
                }

                @Override
                public void onFailure(int reason) {
                    isGroupCreated = false;
                    Log.d(TAG, "P2P Group failed " + reason);
                }
            });
        }else {
            isGroupCreated = false;
        }
    }

    public boolean isGroupCreated(){
        return isGroupCreated;
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        EventBus.getDefault().post(new OnConnectInfoObtainedEvent(info));
        mConnectionInfoState = ConnectionInfoState.AVAILABLE;
        groupOwner = info.isGroupOwner;
        EventBus.getDefault().post(new GroupOwnerEvent(groupOwner));
        Log.e(TAG, "onConnectionInfoAvailable: isGroupOwner : " + groupOwner);
    }

}
