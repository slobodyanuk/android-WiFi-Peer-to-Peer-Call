package com.android.wificall.view.adapter;

import android.app.Activity;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.wificall.R;
import com.android.wificall.util.DeviceUtils;
import com.android.wificall.util.Globals;
import com.android.wificall.util.PrefsKeys;
import com.android.wificall.util.ShowCaseUtils;
import com.android.wificall.util.TimeConstants;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.pixplicity.easyprefs.library.Prefs;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matviy on 12.09.16.
 */
public class PeersAdapter extends RecyclerView.Adapter<PeerHolder> {

    private List<WifiP2pDevice> items;
    private int selectedPos = -1;
    private Callback mCallback;
    private Activity mActivity;
    private Handler handler = new Handler();

    private boolean isSpeaker = Prefs.getBoolean(PrefsKeys.IS_SPEAKER, false);

    public PeersAdapter(Activity activity, List<WifiP2pDevice> items) {
        this.items = new ArrayList<>();
        this.mActivity = activity;
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

            Target viewTarget = new ViewTarget(holder.btnConnect);
            new ShowCaseUtils()
                    .showCaseView(
                            Globals.CONNECTING_BUTTON_CASEVIEW_ID,
                            R.string.showcase_connecting_button_title,
                            R.string.showcase_connecting_button_text,
                            viewTarget, mActivity);

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
        if (!Prefs.getBoolean(PrefsKeys.IS_CONNECTION_VIEW_SHOW, false)) {
            Runnable r = () -> {
                Target mViewTarget = new ViewTarget(holder.name);
                new ShowCaseUtils()
                        .showCaseView(
                                Globals.CONNECTING_CASEVIEW_ID,
                                R.string.showcase_connecting_title,
                                R.string.showcase_connecting_text,
                                mViewTarget, mActivity);
                Prefs.putBoolean(PrefsKeys.IS_CONNECTION_VIEW_SHOW, true);

            };
            handler.postDelayed(r, TimeConstants.SECOND);
        }else {
            handler.removeCallbacksAndMessages(null);
        }

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

