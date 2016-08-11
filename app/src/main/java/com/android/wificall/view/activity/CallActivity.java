package com.android.wificall.view.activity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.android.wificall.R;
import com.android.wificall.data.Client;
import com.android.wificall.data.Packet;
import com.android.wificall.router.Configuration;
import com.android.wificall.router.NetworkManager;
import com.android.wificall.router.Sender;
import com.android.wificall.util.Globals;
import com.android.wificall.util.PermissionsUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import butterknife.BindView;
import butterknife.OnClick;

import static com.android.wificall.router.Configuration.RECEIVE_PORT;
import static com.android.wificall.router.Configuration.RECORDER_AUDIO_ENCODING;
import static com.android.wificall.router.Configuration.RECORDER_CHANNEL_IN;
import static com.android.wificall.router.Configuration.RECORDER_CHANNEL_OUT;
import static com.android.wificall.router.Configuration.RECORDER_RATE;

public class CallActivity extends BaseActivity {

    private static final int MIN_BUFFER_SIZE = 2048;
    private static final int RECORD_BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDER_RATE, RECORDER_CHANNEL_IN, RECORDER_AUDIO_ENCODING);
    private static int RECEIVE_BUFFER_SIZE = AudioTrack.getMinBufferSize(RECORDER_RATE, RECORDER_CHANNEL_OUT, RECORDER_AUDIO_ENCODING);

    private static ArrayList<InetAddress> mAddresses = new ArrayList<>();

    private boolean isGroupOwner;

    private static AudioTrack mAudioTrack;

    @BindView(R.id.start)
    Button mStartButton;
    @BindView(R.id.stop)
    Button mStopButton;
    @BindView(R.id.update)
    Button mUpdateButton;

    private DatagramSocket mSendingSocket = null;
    private DatagramSocket mReceivingSocket = null;

    private AudioRecord mAudioRecord = null;
    private Thread mRecordingThread = null;
    private Thread mReceivingThread = null;

    private boolean isRecording = false;
    private byte[] buffer;
    private boolean isReceiving = false;
    private boolean stopThread = false;

    public static void addJoinedAddress(InetAddress address) {
        if (mAddresses != null) {
            mAddresses.add(address);
            Set<InetAddress> hs = new HashSet<>();
            hs.addAll(mAddresses);
            mAddresses.clear();
            mAddresses.addAll(hs);
            hs.clear();
        }
    }

    public static void removeLeftAddress(InetAddress address) {
        if (mAddresses != null) {
            for (int i = 0; i < mAddresses.size(); i++) {
                if (mAddresses.get(i).equals(address)) {
                    mAddresses.remove(i);
                }
            }
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

        if (RECEIVE_BUFFER_SIZE < MIN_BUFFER_SIZE){
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
        mReceivingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
                if (!isGroupOwner) {
                    initReceiver();
                }
            }
        });
        stopThread = false;
        mReceivingThread.start();
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

    public void initReceiver() {

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                RECORDER_RATE,
                RECORDER_CHANNEL_OUT,
                RECORDER_AUDIO_ENCODING,
                RECEIVE_BUFFER_SIZE * 2,
                AudioTrack.MODE_STREAM);

        AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 65, 0);
        mAudioManager.setParameters("noise_suppression=auto");

        mAudioTrack.play();

        buffer = new byte[RECEIVE_BUFFER_SIZE];

        try {
            mReceivingSocket = new DatagramSocket(RECEIVE_PORT);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        try {
            byte[] routingTable = NetworkManager.serializeRoutingTable();
            Packet ack = new Packet(Packet.TYPE.HELLO_ACK, routingTable, NetworkManager.getSelf().getGroupOwnerMac(), NetworkManager.getSelf()
                    .getMac());
            Sender.queuePacket(ack);
        } catch (Exception e) {
            finish();
        }

        final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        isReceiving = true;
        while (isReceiving && !stopThread) {
            try {
                mReceivingSocket.receive(packet);
                mAudioTrack.write(packet.getData(), 0, packet.getLength());

            } catch (IOException e) {
                Log.e("VR", "IOException");
                isReceiving = false;
                stopThread = true;
                break;
            } catch (Exception e) {
                e.printStackTrace();
                stopThread = true;
                isReceiving = false;
                break;
            }
        }
        if (mReceivingSocket != null) {
            mReceivingSocket.disconnect();
            mReceivingSocket.close();
        }

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
    }

    private void startRecording() {

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                RECORDER_RATE, RECORDER_CHANNEL_IN,
                RECORDER_AUDIO_ENCODING, RECORD_BUFFER_SIZE);
        mAudioRecord.startRecording();
        isRecording = true;

        for (Client c : NetworkManager.routingTable.values()) {
            if (c.getMac().equals(NetworkManager.getSelf().getMac()))
                continue;
            try {
                mAddresses.add(InetAddress.getByName(c.getIp()));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        try {
            mSendingSocket = new DatagramSocket();
        } catch (SocketException e) {
            return;
        }

        mRecordingThread = new Thread(new Runnable() {
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
                writeAudioData();
            }
        });
        stopThread = false;
        mRecordingThread.start();
    }

    private void writeAudioData() {
        byte[] byteData = new byte[RECORD_BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(byteData, byteData.length);

        while (isRecording && !stopThread) {
            mAudioRecord.read(byteData, 0, byteData.length);

            for (int i = 0; i < mAddresses.size(); i++) {
                try {
                    packet.setAddress(mAddresses.get(i));
                    packet.setData(byteData);
                    packet.setLength(byteData.length);
                    packet.setPort(Configuration.RECEIVE_PORT);
                    if (mSendingSocket != null) {
                        mSendingSocket.send(packet);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private void stopRecording() {
        if (null != mAudioRecord) {
            isRecording = false;
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
            mRecordingThread.interrupt();
            mRecordingThread = null;
        }
        if (mSendingSocket != null) {
            mSendingSocket.disconnect();
            mSendingSocket.close();
            mSendingSocket = null;
        }
    }

    private void stopReceiving() {
        isReceiving = false;
        buffer = null;
        stopThread = true;

        if (mReceivingThread != null) {
            mReceivingThread.interrupt();
            mReceivingThread = null;
        }
        if (mReceivingSocket != null) {
            mReceivingSocket.disconnect();
            mReceivingSocket.close();
            mReceivingSocket = null;
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
            stopReceiving();
            stopRecording();
            enableButtons(isRecording);
        }
    }

    private void enableButtons(boolean isRecording) {
        enableButton(mStartButton, !isRecording);
        enableButton(mStopButton, isRecording);
    }

    private void enableButton(Button button, boolean isEnable) {
        button.setEnabled(isEnable);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.call;
    }
}