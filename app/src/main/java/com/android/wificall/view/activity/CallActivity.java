package com.android.wificall.view.activity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.View;
import android.widget.Button;

import com.android.wificall.R;
import com.android.wificall.data.Packet;
import com.android.wificall.router.NetworkManager;
import com.android.wificall.router.Sender;
import com.android.wificall.router.audio.AudioReader;
import com.android.wificall.router.audio.AudioRecorder;
import com.android.wificall.util.Globals;
import com.android.wificall.util.PermissionsUtil;
import com.android.wificall.util.TimeConstants;

import java.net.InetAddress;

import butterknife.BindView;
import butterknife.OnClick;

import static com.android.wificall.router.Configuration.RECORDER_AUDIO_ENCODING;
import static com.android.wificall.router.Configuration.RECORDER_CHANNEL_IN;
import static com.android.wificall.router.Configuration.RECORDER_CHANNEL_OUT;
import static com.android.wificall.router.Configuration.RECORDER_RATE;

public class CallActivity extends BaseActivity {

    private static final int MIN_BUFFER_SIZE = 2048;
    private static int RECORD_BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDER_RATE, RECORDER_CHANNEL_IN, RECORDER_AUDIO_ENCODING);
    private static int RECEIVE_BUFFER_SIZE = AudioTrack.getMinBufferSize(RECORDER_RATE, RECORDER_CHANNEL_OUT, RECORDER_AUDIO_ENCODING);


    private boolean isGroupOwner;


    @BindView(R.id.start)
    Button mStartButton;
    @BindView(R.id.stop)
    Button mStopButton;
    @BindView(R.id.update)
    Button mUpdateButton;

    private AudioTrack mAudioTrack = null;
    private AudioReader mAudioReader;
    private static AudioRecorder mAudioRecorder;

    private Thread mRecordingThread = null;
    private Thread mReceivingThread = null;

    private boolean stopThread = false;

    private boolean isUpdating;

    private final Handler mHandler = new Handler();
    private Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
                if (isUpdating) {
                    onConnectionUpdate();
                    mHandler.postDelayed(mUpdateRunnable, TimeConstants.MINUTE);
            }
        }
    };

    public static void addJoinedAddress(InetAddress address) {
        if (mAudioRecorder != null) {
            mAudioRecorder.setAddress(address);
        }
    }

    public static void removeLeftAddress(InetAddress address) {
        if (mAudioRecorder != null) {
            mAudioRecorder.removeAddress(address);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isGroupOwner && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && PermissionsUtil.needRecordAudioPermissions(this)) {
            PermissionsUtil.requestRecordAudioPermission(this);
            mStartButton.setVisibility(View.GONE);
            mStopButton.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("group_owner", WifiDirectActivity.isGroupOwner);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        isGroupOwner = savedInstanceState.getBoolean("group_owner");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            isGroupOwner = savedInstanceState.getBoolean("group_owner");
        } else {
            isGroupOwner = WifiDirectActivity.isGroupOwner;
        }

        isUpdating = true;
        mHandler.postDelayed(mUpdateRunnable, TimeConstants.SECOND);

        if (RECEIVE_BUFFER_SIZE < MIN_BUFFER_SIZE) {
            RECEIVE_BUFFER_SIZE = MIN_BUFFER_SIZE;
        }

        if (!isGroupOwner) {
            mStartButton.setVisibility(View.GONE);
            mStopButton.setVisibility(View.GONE);
            mUpdateButton.setVisibility(View.VISIBLE);
        } else {
            mUpdateButton.setVisibility(View.GONE);
        }

        enableButtons(false);
        initReceivingThread();
    }

    private void initReceivingThread() {
        if (!isGroupOwner) {
            mAudioReader = new AudioReader(CallActivity.this, RECEIVE_BUFFER_SIZE);
            mReceivingThread = new Thread(mAudioReader);
            stopThread = false;
            mReceivingThread.start();
        }
    }

    @OnClick(R.id.start)
    public void onStartClick() {
        enableButtons(true);
        startRecording();
    }

    @OnClick(R.id.stop)
    public void onStopClick() {
        enableButtons(false);
        stopRecording();
    }

    @OnClick(R.id.update)
    public void onUpdateClick() {
        mAudioTrack = mAudioReader.getAudioTrack();

        if (mAudioTrack != null) {
            byte[] rtable = NetworkManager.serializeRoutingTable();
            Packet ack = new Packet(Packet.TYPE.HELLO_ACK, rtable, NetworkManager.getSelf().getGroupOwnerMac(), NetworkManager.getSelf()
                    .getMac());
            Sender.queuePacket(ack);

            stopReceiving();
            if (mAudioTrack != null && mAudioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
                if (mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
                    try {
                        mAudioTrack.flush();
                        mAudioTrack.stop();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
                mAudioTrack.release();
            }
            initReceivingThread();
        }
    }

    private void startRecording() {

        mAudioRecorder = new AudioRecorder(this, RECORD_BUFFER_SIZE);
        mRecordingThread = new Thread(mAudioRecorder);
        stopThread = false;
        mRecordingThread.start();
    }

    public boolean isThreadStopped() {
        return stopThread;
    }

    public void setThreadStopped(boolean stop) {
        stopThread = stop;
    }

    private void stopRecording() {
        if (mAudioRecorder != null) {
            mAudioRecorder.stopRecording();
        }
        if (mReceivingThread != null) {
            mRecordingThread.interrupt();
            mRecordingThread = null;
        }
    }

    private void stopReceiving() {
        if (mAudioReader != null) {
            mAudioReader.stopReceiving();
            stopThread = true;
        }

        if (mReceivingThread != null) {
            mReceivingThread.interrupt();
            mReceivingThread = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == Globals.REQUEST_RECORD_AUDIO) {
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                mStartButton.setVisibility(View.VISIBLE);
                mStopButton.setVisibility(View.VISIBLE);
            } else {
                finish();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        boolean screenOn;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            screenOn = pm.isInteractive();
        } else {
            screenOn = pm.isScreenOn();
        }

        if (screenOn) {
            stopUpdating();
            stopReceiving();
            stopRecording();
            enableButtons(false);
        }
    }

    private void enableButtons(boolean isRecording) {
        enableButton(mStartButton, !isRecording);
        enableButton(mStopButton, isRecording);
    }


    private void enableButton(Button button, boolean isEnable) {
        button.setEnabled(isEnable);
    }

    private void stopUpdating() {
        isUpdating = false;
        mHandler.removeCallbacksAndMessages(null);
    }


    @Override
    protected int getLayoutResource() {
        return R.layout.call;
    }

    public void onConnectionUpdate() {
        onUpdateClick();
    }
}