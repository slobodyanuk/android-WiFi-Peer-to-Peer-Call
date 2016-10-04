package com.android.wificall.view.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.wificall.App;
import com.android.wificall.R;
import com.android.wificall.data.Client;
import com.android.wificall.data.Packet;
import com.android.wificall.data.event.DisconnectEvent;
import com.android.wificall.data.event.LeaveGroupEvent;
import com.android.wificall.data.event.OnConnectInfoObtainedEvent;
import com.android.wificall.data.event.RequestPeersEvent;
import com.android.wificall.data.event.SomebodyJoinedEvent;
import com.android.wificall.data.event.SomebodyLeftEvent;
import com.android.wificall.data.event.UpdateDeviceEvent;
import com.android.wificall.data.event.UpdateRoomInfoEvent;
import com.android.wificall.data.event.WifiP2PStateChangedEvent;
import com.android.wificall.router.NetworkManager;
import com.android.wificall.router.Sender;
import com.android.wificall.router.broadcast.WifiDirectBroadcastReceiver;
import com.android.wificall.util.DeviceActionListener;
import com.android.wificall.util.DeviceUtils;
import com.android.wificall.util.Globals;
import com.android.wificall.util.PrefsKeys;
import com.android.wificall.util.ShowCaseUtils;
import com.android.wificall.util.TimeConstants;
import com.android.wificall.view.adapter.PeersAdapter;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.pixplicity.easyprefs.library.Prefs;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

import static com.android.wificall.util.Globals.CALLING_CASEVIEW_ID;
import static com.android.wificall.util.Globals.FIND_CASEVIEW_ID;
import static com.android.wificall.util.Globals.LEAVE_CASEVIEW_ID;

public class WifiDirectActivity extends BaseActivity
        implements WifiP2pManager.ChannelListener, DeviceActionListener,
        WifiP2pManager.PeerListListener, View.OnTouchListener, ShowCaseUtils.CaseViewListener {

    private static String TAG = WifiDirectActivity.class.getClass().getName();
    private final IntentFilter mIntentFilter = new IntentFilter();

    @BindView(R.id.btn_call)
    ImageButton mCallButton;
    @BindView(R.id.btn_discover)
    ImageButton mDiscoverButton;

    //current device info
    private WifiP2pDevice mDevice = null;
    @BindView(R.id.my_name)
    TextView tvCurrentName;
    @BindView(R.id.my_status)
    TextView tvCurrentStatus;

    //peers
    @BindView(R.id.recycler)
    RecyclerView mRecyclerView;
    @BindView(R.id.rl_peers_list)
    RelativeLayout rlPeersList;
    @BindView(R.id.tv_no_peers)
    TextView tvNoPeers;

    //connection info
    @BindView(R.id.rl_connection_info)
    RelativeLayout rlConnectionInfo;
    @BindView(R.id.device_address)
    TextView tvDeviceAddress;
    @BindView(R.id.device_info)
    TextView tvDeviceInfo;
    @BindView(R.id.group_owner)
    TextView tvGroupOwner;
    @BindView(R.id.group_ip)
    TextView tvGroupIp;

    private LinearLayoutManager mLayoutManager;
    private PeersAdapter mAdapter;

    private boolean isVisible = false;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;

    private WifiP2pManager mWifiManager;
    private WifiManager mWifiNetworkManager;
    private WifiP2pManager.Channel mWifiChannel;
    private WifiDirectBroadcastReceiver mReceiver = null;

    private boolean needCreateGroup = false;
    private boolean isSpeaker = false;

    //peers list fragment
    private List<WifiP2pDevice> mPeers = new ArrayList<>();
    private List<WifiP2pDevice> mGroupOwners = new ArrayList<>();

    private ProgressDialog mProgressDialog = null;
    private ProgressDialog mConnectingDialog = null;
    private ProgressDialog mDiscoveringDialog = null;
    private boolean caseShowed = false;

    private final Handler mHandler = new Handler();
    private boolean isUpdating;
    private Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (isUpdating) {
                dismissConnectingProgress();
                mHandler.postDelayed(mUpdateRunnable, TimeConstants.THRITY_SECONDS);
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
//        showLeaveCaseView();
        mReceiver = new WifiDirectBroadcastReceiver(mWifiManager, mWifiChannel);
        registerReceiver(mReceiver, mIntentFilter);
        isVisible = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isUpdating = true;
        mHandler.postDelayed(mUpdateRunnable, TimeConstants.THRITY_SECONDS);

        isSpeaker = Prefs.getBoolean(PrefsKeys.IS_SPEAKER, false);

        setTitle(getString(R.string.find_group_title, isSpeaker ? "Speaker" : "Listener"));
        initRouterSettings();
        initRecycler();

        if (isSpeaker) {
            needCreateGroup = true;
            Toast.makeText(WifiDirectActivity.this, "speaker", Toast.LENGTH_SHORT).show();
        } else {
            needCreateGroup = false;
            Toast.makeText(WifiDirectActivity.this, "listener", Toast.LENGTH_SHORT).show();
        }

    }

    private void initRouterSettings() {
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mWifiManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mWifiNetworkManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWifiChannel = mWifiManager.initialize(this, getMainLooper(), null);
        ((App) getApplication()).setWifiManager(mWifiManager, mWifiChannel);
    }

    private void initRecycler() {
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new PeersAdapter(this, null);
        mAdapter.setCallback(device -> {
            connect(DeviceUtils.getConfig(device));
            showConnectingProgress(device.deviceAddress);
        });
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    public void setWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    public void resetData() {
        mPeers.clear();
        mGroupOwners.clear();
        mAdapter.setItems(mPeers);
        onDisconnected();
    }

    @OnClick(R.id.btn_leave)
    public void leave() {
        ((App) getApplication()).deletePersistentGroups();
        Sender.queuePacket(new Packet(Packet.TYPE.BYE, new byte[0], NetworkManager.getSelf().getGroupOwnerMac(), NetworkManager.getSelf().getMac()));
        disconnect();
    }

    @OnClick(R.id.btn_discover)
    public void onDiscoverClick() {
        if (!isWifiP2pEnabled) {
            Toast.makeText(this, R.string.p2p_off_warning, Toast.LENGTH_SHORT).show();
        }
        showDiscoveringProgress();
        mWifiManager.discoverPeers(mWifiChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Toast.makeText(WifiDirectActivity.this, "Discovery Initiated", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reasonCode) {
                Toast.makeText(WifiDirectActivity.this, "Discovery Failed: " + reasonCode, Toast.LENGTH_SHORT).show();
                hideDiscoveringProgress();
            }
        });
    }

    @OnClick(R.id.btn_call)
    public void onCallClick() {
        Intent i = new Intent(getApplicationContext(), CallActivity.class);
        startActivity(i);
    }

    @Override
    public void showDetails(WifiP2pDevice device) {
    }

    private void cancelFindingPeers() {
        showProgress();
        mWifiManager.stopPeerDiscovery(mWifiChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Toast.makeText(WifiDirectActivity.this, "Aborting discovering", Toast.LENGTH_SHORT).show();
                hideProgress();
            }

            @Override
            public void onFailure(int reasonCode) {
                Toast.makeText(WifiDirectActivity.this,
                        "Connect abort request failed. Reason Code: " + reasonCode, Toast.LENGTH_SHORT).show();
                enableWifi();
                hideProgress();
            }
        });
    }

    @Override
    public void cancelDisconnect() {
        if (mWifiManager != null) {
            if (mDevice == null || mDevice.status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } else if (mDevice.status == WifiP2pDevice.AVAILABLE
                    || mDevice.status == WifiP2pDevice.INVITED) {

                showProgress();
                mWifiManager.cancelConnect(mWifiChannel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(WifiDirectActivity.this, "Aborting connection", Toast.LENGTH_SHORT).show();
                        hideProgress();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WifiDirectActivity.this,
                                "Connect abort request failed. Reason Code: " + reasonCode, Toast.LENGTH_SHORT).show();
                        enableWifi();
                        hideProgress();
                    }
                });
            }
        }
    }

    @Override
    public void connect(WifiP2pConfig config) {
        isUpdating = true;
        mHandler.postDelayed(mUpdateRunnable, TimeConstants.THRITY_SECONDS);
        mWifiManager.connect(mWifiChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reason) {
                String reasonStr = "Connect failed. Retry. Try Disable/Re-Enable Wi-Fi.";
                switch (reason) {
                    case WifiManager.ERROR_AUTHENTICATING:
                        reasonStr = reasonStr + " ERROR_AUTHENTICATING";
                        break;
                    case WifiManager.WPS_AUTH_FAILURE:
                        reasonStr = reasonStr + " WPS_AUTH_FAILURE";
                        break;
                    case WifiManager.WPS_OVERLAP_ERROR:
                        reasonStr = reasonStr + " WPS_OVERLAP_ERROR";
                        break;
                    case WifiManager.WPS_TIMED_OUT:
                        reasonStr = reasonStr + " WPS_TIMED_OUT";
                        break;
                    case WifiManager.WPS_TKIP_ONLY_PROHIBITED:
                        reasonStr = reasonStr + " WPS_TKIP_ONLY_PROHIBITED";
                        break;
                    case WifiManager.WPS_WEP_PROHIBITED:
                        reasonStr = reasonStr + " WPS_WEP_PROHIBITED";
                        break;
                    default:
                        reasonStr = reasonStr + " unknown reason";
                        break;
                }
                Toast.makeText(WifiDirectActivity.this, reasonStr, Toast.LENGTH_SHORT).show();
                isUpdating = false;
                mHandler.removeCallbacksAndMessages(null);
                hideConnectingProgress();
                enableWifi();
            }
        });
    }

    @Override
    public void disconnect() {
        if (mWifiManager != null && mWifiChannel != null) {
            showProgress();
            mWifiManager.requestGroupInfo(mWifiChannel, group -> {
                if (group != null && mWifiManager != null && mWifiChannel != null) {
                    mWifiManager.removeGroup(mWifiChannel, new WifiP2pManager.ActionListener() {

                        @Override
                        public void onSuccess() {
                            Log.e(TAG, "removeGroup onSuccess -");
                            onDisconnected();
                        }

                        @Override
                        public void onFailure(int reason) {
                            Log.e(TAG, "removeGroup onFailure -" + reason);
                        }
                    });
                } else {
                    onDisconnected();
                }
            });
        }
    }

    private void showLeaveCaseView() {
        Target viewTarget = new ViewTarget(R.id.btn_leave, this);
        new ShowCaseUtils(this)
                .showCaseView(
                        LEAVE_CASEVIEW_ID,
                        R.string.showcase_leave_title,
                        R.string.showcase_leave_text,
                        viewTarget, this);
    }

    private void showFindCaseView(int id) {
        Target viewTarget = new ViewTarget(R.id.btn_discover, this);
        new ShowCaseUtils(this)
                .showCaseView(
                        id,
                        R.string.showcase_finding_title,
                        R.string.showcase_finding_text,
                        viewTarget, this);
    }

    private void showStartCallingView(int id) {
        Target viewTarget = new ViewTarget(R.id.btn_call, this);
        new ShowCaseUtils(this)
                .showCaseView(
                        id,
                        R.string.showcase_calling_title,
                        R.string.showcase_calling_text,
                        viewTarget, this);
    }

    @Override
    public void onCaseViewDidHide(int id) {
        switch (id) {
            case CALLING_CASEVIEW_ID:
                showLeaveCaseView();
                break;
            case FIND_CASEVIEW_ID:
                showLeaveCaseView();
                break;
        }
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
    protected void onDestroy() {
        super.onDestroy();
        isUpdating = false;
        mHandler.removeCallbacksAndMessages(null);
    }

    public boolean isVisible() {
        return isVisible;
    }

    private void enableWifi() {
        if (mWifiNetworkManager != null) {
            mWifiNetworkManager.setWifiEnabled(false);
            mWifiNetworkManager.setWifiEnabled(true);
        }
    }

    @Override
    public void onConnected() {
        if (mAdapter != null) {
            mAdapter.clearSelection();
        }
        mDiscoverButton.setVisibility(View.GONE);
        mCallButton.setVisibility(View.VISIBLE);
        rlPeersList.setVisibility(View.GONE);
        rlConnectionInfo.setVisibility(View.VISIBLE);
        if (!isSpeaker) {
            showStartCallingView(Globals.CALLING_SPEAKER_CASEVIEW_ID);
        }
        hideNoPeers();
        hideProgress();
    }

    @Override
    public void onDisconnected() {
        mDiscoverButton.setVisibility(View.VISIBLE);
        mCallButton.setVisibility(View.GONE);
        rlPeersList.setVisibility(View.VISIBLE);
        rlConnectionInfo.setVisibility(View.GONE);
        if (!isSpeaker && !caseShowed) {
            showFindCaseView(Globals.FIND_SPEAKER_CASEVIEW_ID);
            caseShowed = true;
        }
        showNoPeers();
        hideProgress();
    }

    //here replacements from DeviceListFragment
    public void updateThisDevice(WifiP2pDevice device) {
        this.mDevice = device;
        tvCurrentName.setText(device.deviceName);
        tvCurrentStatus.setText(DeviceUtils.getDeviceStatus(device.status));
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        hideDiscoveringProgress();

        mPeers.clear();
        mGroupOwners.clear();
        Collection<WifiP2pDevice> deviceList = peers.getDeviceList();
        mPeers.addAll(deviceList);

        for (int peer = 0; peer < mPeers.size(); peer++) {
            if (mPeers.get(peer).isGroupOwner()) {
                mGroupOwners.add(mPeers.get(peer));
            }
        }

        if (mAdapter != null) {
            if (isSpeaker || mGroupOwners.size() == 0) {
                mAdapter.setItems(mPeers);
            } else {
                mAdapter.setItems(mGroupOwners);
            }
        }

        if (mPeers.size() == 0) {
            Log.d(TAG, "No devices found");
            showNoPeers();
        } else {
            hideNoPeers();
        }
    }

    private void showNoPeers() {
        tvNoPeers.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
    }

    private void hideNoPeers() {
        if (mPeers != null && mPeers.size() > 0) {
            tvNoPeers.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void hideProgress() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    private void showProgress() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Press back to cancel");
        mProgressDialog.setMessage("Progress");
        mProgressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "Cancel", (dialogInterface, i) -> {
            dialogInterface.dismiss();
        });
        mProgressDialog.show();
    }

    private void hideDiscoveringProgress() {
        if (mDiscoveringDialog != null && mDiscoveringDialog.isShowing()) {
            mDiscoveringDialog.dismiss();
        }
    }

    private void showDiscoveringProgress() {
        if (mDiscoveringDialog != null && mDiscoveringDialog.isShowing()) {
            mDiscoveringDialog.dismiss();
        }
        mDiscoveringDialog = new ProgressDialog(this);
        mDiscoveringDialog.setTitle("Please wait...");
        mDiscoveringDialog.setMessage("Discovering available peers");
        mDiscoveringDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "Cancel", (dialogInterface, i) -> {
            cancelFindingPeers();
            dialogInterface.dismiss();
        });
        mDiscoveringDialog.show();
    }

    private void showConnectingProgress(String deviceAddress) {
        if (mConnectingDialog != null && mConnectingDialog.isShowing()) {
            mConnectingDialog.dismiss();
        }
        mConnectingDialog = new ProgressDialog(this);
        mConnectingDialog.setTitle("Press back to cancel");
        mConnectingDialog.setMessage("Connecting to :" + deviceAddress);
        mConnectingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "Cancel", (dialogInterface, i) -> {
            dialogInterface.dismiss();
            cancelDisconnect();
        });
        mConnectingDialog.show();
    }

    private void hideConnectingProgress() {
        if (mConnectingDialog != null && mConnectingDialog.isShowing()) {
            mConnectingDialog.hide();

        }
    }

    private void dismissConnectingProgress() {
        if (mConnectingDialog != null && mConnectingDialog.isShowing()) {
            mConnectingDialog.dismiss();
            cancelDisconnect();
        }
    }

    public void updateGroupChatMembersMessage() {
        if (tvDeviceAddress != null) {
            String s = "Currently in this room: \n";
            for (Client c : NetworkManager.routingTable.values()) {
                s += c.getName() + "\n";
            }
            tvDeviceAddress.setText(s);
        }
    }

    //single top activity
    public static void invoke(Context context) {
        Intent intent = new Intent(context, WifiDirectActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Subscribe
    public void onEvent(LeaveGroupEvent event) {
        if (!isSpeaker) {
            leave();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(UpdateDeviceEvent event) {
        isUpdating = false;
        mHandler.removeCallbacksAndMessages(null);
        updateThisDevice(event.getDevice());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DisconnectEvent event) {
        resetData();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(WifiP2PStateChangedEvent event) {
        setWifiP2pEnabled(event.isEnabled());
        if (needCreateGroup && !mReceiver.isGroupCreated() && event.isEnabled()) {
            mReceiver.createGroup();
        }
        if (!event.isEnabled()) {
            resetData();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(RequestPeersEvent event) {
        if (mWifiManager != null && mWifiChannel != null) {
            mWifiManager.requestPeers(mWifiChannel, this);
        }
        updateGroupChatMembersMessage();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(OnConnectInfoObtainedEvent event) {
        hideConnectingProgress();

        if (!event.getInfo().isGroupOwner) {
            Sender.queuePacket(new Packet(Packet.TYPE.HELLO, new byte[0], null, WifiDirectBroadcastReceiver.MAC));
        }

        if (NetworkManager.routingTable.size() <= 1 && !isSpeaker) {
            //only this device in this group
            disconnect();
        } else {
            onConnected();
        }

        if (isSpeaker) {
            showStartCallingView(Globals.CALLING_CASEVIEW_ID);
        } else {
            showFindCaseView(Globals.FIND_SPEAKER_CASEVIEW_ID);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SomebodyJoinedEvent event) {
        Toast.makeText(WifiDirectActivity.this, "somebody joined event", Toast.LENGTH_SHORT).show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SomebodyLeftEvent event) {
        Toast.makeText(WifiDirectActivity.this, "somebody left event", Toast.LENGTH_SHORT).show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(UpdateRoomInfoEvent event) {
        updateGroupChatMembersMessage();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            if (view instanceof ShowcaseView) {
                ((ShowcaseView) view).hide();
            }
        }
        return true;
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

}
