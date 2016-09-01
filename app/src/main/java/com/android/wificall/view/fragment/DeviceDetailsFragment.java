package com.android.wificall.view.fragment;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.wificall.R;
import com.android.wificall.data.Client;
import com.android.wificall.data.Packet;
import com.android.wificall.router.NetworkManager;
import com.android.wificall.router.Sender;
import com.android.wificall.router.broadcast.WifiDirectBroadcastReceiver;
import com.android.wificall.util.DeviceActionListener;

import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by slobodyanuk on 11.07.16.
 */
public class DeviceDetailsFragment extends Fragment implements WifiP2pManager.ConnectionInfoListener {

    private static View mContentView = null;
    private WifiP2pDevice mDevice;
    private ProgressDialog mProgressDialog = null;
    private Unbinder bind;

    public static void updateGroupChatMembersMessage() {
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        if (view != null) {
            String s = "Currently in the network chatting: \n";
            for (Client c : NetworkManager.routingTable.values()) {
                s += c.getMac() + "\n";
            }
            view.setText(s);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.device_detail, container, false);
        bind = ButterKnife.bind(this, mContentView);

        ButterKnife.findById(mContentView, R.id.btn_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = mDevice.deviceAddress;
                config.groupOwnerIntent = 0;
                config.wps.setup = WpsInfo.PBC;
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
                mProgressDialog = ProgressDialog.show(getActivity(), "Press back to cancel", "Connecting to :"
                        + mDevice.deviceAddress, true, true);
                ((DeviceActionListener) getActivity()).connect(config);
            }
        });

        ButterKnife.findById(mContentView, R.id.btn_disconnect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sender.queuePacket(new Packet(Packet.TYPE.BYE, new byte[0], NetworkManager.getSelf().getGroupOwnerMac(),  NetworkManager.getSelf().getMac()));
                ((DeviceActionListener) getActivity()).disconnect();
            }
        });

        return mContentView;
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        this.getView().setVisibility(View.VISIBLE);

        if (!info.isGroupOwner) {
            Sender.queuePacket(new Packet(Packet.TYPE.HELLO, new byte[0], null, WifiDirectBroadcastReceiver.MAC));
        }

        // hide the connect button
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }

    public void showDetails(WifiP2pDevice device) {
        this.mDevice = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        String s = "Currently in the network chatting: \n";
        for (Client c : NetworkManager.routingTable.values()) {
            s += c.getMac() + "\n";
        }

        view.setText(s);
    }

    public void resetViews() {
        ButterKnife.findById(mContentView, R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = ButterKnife.findById(mContentView, R.id.device_address);
        view.setText(R.string.empty);
        view = ButterKnife.findById(mContentView, R.id.device_info);
        view.setText(R.string.empty);
        view = ButterKnife.findById(mContentView, R.id.group_owner);
        view.setText(R.string.empty);
        view = ButterKnife.findById(mContentView, R.id.status_text);
        view.setText(R.string.empty);
        this.getView().setVisibility(View.GONE);
    }

    public void dismissDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind.unbind();
    }

}
