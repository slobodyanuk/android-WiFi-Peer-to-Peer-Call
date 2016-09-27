package com.android.wificall.view.adapter;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.wificall.R;
import com.android.wificall.util.DeviceUtils;

import java.util.List;

/**
 * Created by slobodyanuk on 11.07.16.
 */
public class WifiPeerListAdapter extends ArrayAdapter<WifiP2pDevice> {

    private List<WifiP2pDevice> items;
    private Context mContext;

    public WifiPeerListAdapter(Context context, int textViewResourceId, List<WifiP2pDevice> objects) {
        super(context, textViewResourceId, objects);
        items = objects;
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.row_devices, null);
        }
        WifiP2pDevice device = items.get(position);
        if (device != null) {
            TextView top = (TextView) v.findViewById(R.id.device_name);
            TextView bottom = (TextView) v.findViewById(R.id.device_details);
            if (top != null) {
                top.setText(device.deviceName);
            }
            if (bottom != null) {
                bottom.setText(DeviceUtils.getDeviceStatus(device.status));
            }
        }
        return v;
    }
}
