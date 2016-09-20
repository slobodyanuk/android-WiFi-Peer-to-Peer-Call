package com.android.wificall.view.fragment;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.android.wificall.R;
import com.android.wificall.util.DeviceActionListener;
import com.android.wificall.view.adapter.WifiPeerListAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by slobodyanuk on 11.07.16.
 */
public class DeviceListFragment extends ListFragment implements WifiP2pManager.PeerListListener {

    private static final String TAG = DeviceListFragment.class.getCanonicalName();

    private List<WifiP2pDevice> mPeers = new ArrayList<WifiP2pDevice>();
    private List<WifiP2pDevice> mGroupOwners = new ArrayList<WifiP2pDevice>();
    private ProgressDialog mProgressDialog = null;
    private View mContentView = null;
    private WifiP2pDevice mDevice;

    public static String getDeviceStatus(int deviceStatus) {
        Log.d(TAG, "Peer status :" + deviceStatus);
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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.setListAdapter(new WifiPeerListAdapter(getActivity(), R.layout.row_devices, mGroupOwners));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.device_list, null);
        return mContentView;
    }

    public WifiP2pDevice getDevice() {
        return mDevice;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        WifiP2pDevice device = (WifiP2pDevice) getListAdapter().getItem(position);
        ((DeviceActionListener) getActivity()).showDetails(device);
    }

    public void updateThisDevice(WifiP2pDevice device) {
        this.mDevice = device;
        TextView view = (TextView) mContentView.findViewById(R.id.my_name);
        view.setText(device.deviceName);
        view = (TextView) mContentView.findViewById(R.id.my_status);
        view.setText(getDeviceStatus(device.status));
    }


    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        mPeers.clear();
        mGroupOwners.clear();
        mPeers.addAll(peers.getDeviceList());

        for (int peer = 0; peer < mPeers.size(); peer++) {
            if (mPeers.get(peer).isGroupOwner()) {
                mGroupOwners.add(mPeers.get(peer));
            }
        }

        if (mGroupOwners.size() == 0 && mPeers.size() != 0) {
            mGroupOwners.addAll(mPeers);
        }

        ((WifiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
        if (mPeers.size() == 0) {
            Log.d(TAG, "No devices found");
            return;
        }
    }

    public void clearPeers() {
        mPeers.clear();
        mGroupOwners.clear();
        ((WifiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
    }

    public void onInitiateDiscovery() {

        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        mProgressDialog = ProgressDialog.show(getActivity(), "Press back to cancel", "finding peers", true, true,
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {

                    }
                });
    }

    public void dismissDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

}
