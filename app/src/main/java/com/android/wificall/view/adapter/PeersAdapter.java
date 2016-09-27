package com.android.wificall.view.adapter;

import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pDevice;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.wificall.R;
import com.android.wificall.util.DeviceUtils;
import com.android.wificall.util.PrefsKeys;
import com.pixplicity.easyprefs.library.Prefs;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by matviy on 12.09.16.
 */
public class PeersAdapter extends RecyclerView.Adapter<PeerHolder> {

    private List<WifiP2pDevice> items;
    private int selectedPos = -1;
    private Callback mCallback;
    private boolean isSpeaker = Prefs.getBoolean(PrefsKeys.IS_SPEAKER, false);

    public PeersAdapter(List<WifiP2pDevice> items) {
        this.items = new ArrayList<>();
        if (items != null) {
            this.items.addAll(items);
        }
    }

    public interface Callback {
        void onConnectClick(WifiP2pDevice device);
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public void setItems(List<WifiP2pDevice> peers) {
        if (peers != null) {
            if (items != null) {
                items.clear();
            } else {
                items = new ArrayList<>();
            }
            items.addAll(peers);
            notifyDataSetChanged();
        }
    }

    @Override
    public PeerHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new PeerHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_recycler_peer, parent, false));
    }

    @Override
    public void onBindViewHolder(final PeerHolder holder, final int position) {
        final WifiP2pDevice device = items.get(position);
        if (device != null) {
            holder.name.setText(device.deviceName + (device.isGroupOwner() ? " (Group owner)" : ""));
            holder.tvMac.setText(DeviceUtils.getDeviceStatus(device.status));
            holder.btnConnect.setText(isSpeaker ? "invite" : "connect");
        }

        if (selectedPos == position) {
            holder.highlight();
            holder.tvAddress.setVisibility(View.VISIBLE);
            holder.tvAddress.setText(device.deviceAddress);
            holder.btnConnect.setVisibility(View.VISIBLE);
            holder.btnConnect.setOnClickListener(view -> {
                if (mCallback != null) {
                    mCallback.onConnectClick(device);
                }
            });
        } else {
            holder.unhighlight();
            holder.tvAddress.setVisibility(View.GONE);
            holder.tvAddress.setText("");
            holder.btnConnect.setVisibility(View.GONE);
        }

        holder.root.setOnClickListener(view -> {
            int oldPos = selectedPos;
            if (position == selectedPos) {
                selectedPos = -1;
            } else {
                selectedPos = position;
                notifyItemChanged(position);
            }
            if (oldPos != -1) {
                notifyItemChanged(oldPos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public void clearSelection() {
        int oldPos = selectedPos;
        selectedPos = -1;
        if (oldPos >= 0 && oldPos < getItemCount()) {
            notifyItemChanged(oldPos);
        }
    }

}

class PeerHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.root)
    RelativeLayout root;
    @BindView(R.id.tv_name)
    TextView name;
    @BindView(R.id.tv_mac)
    TextView tvMac;
    @BindView(R.id.tv_device_address)
    TextView tvAddress;
    @BindView(R.id.btn_connect)
    AppCompatButton btnConnect;

    private String defaultColor = "#ffffff";
    private String selectedColor = "#5b9c33";

    public PeerHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public void highlight() {
        root.setBackgroundColor(Color.parseColor(selectedColor));
    }

    public void unhighlight() {
        root.setBackgroundColor(Color.parseColor(defaultColor));
    }

}
