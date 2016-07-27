package com.android.wificall.view.activity;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.android.wificall.R;
import com.android.wificall.data.Client;
import com.android.wificall.router.Configuration;
import com.android.wificall.router.NetworkManager;

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

    private static final int RECORD_BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDER_RATE, RECORDER_CHANNEL_IN, RECORDER_AUDIO_ENCODING);
    private static final int RECEIVE_BUFFER_SIZE = AudioTrack.getMinBufferSize(RECORDER_RATE, RECORDER_CHANNEL_OUT, RECORDER_AUDIO_ENCODING);

    private static ArrayList<InetAddress> mAddresses = new ArrayList<>();

    private static AudioTrack mAudioTrack;

    @BindView(R.id.start)
    Button mStartButton;
    @BindView(R.id.stop)
    Button mStopButton;

    private DatagramSocket mSendingSocket = null;
    private DatagramSocket mReceivingSocket = null;

    private AudioRecord mAudioRecord = null;
    private Thread mRecordingThread = null;
    private Thread mReceivingThread = null;

    private boolean isRecording = false;
    private byte byteData[];
    private AudioManager mAudioManager;
    private byte[] buffer;
    private boolean isReceiving = false;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!WifiDirectActivity.isGroupOwner) {
            mStartButton.setVisibility(View.GONE);
            mStopButton.setVisibility(View.GONE);
        }

        enableButtons(false);

        mReceivingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (Client c : NetworkManager.routingTable.values()) {
                    if (c.getMac().equals(NetworkManager.getSelf().getMac())) {
                        continue;
                    }
                    initReceiver();
                }
            }
        });

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

    public void initReceiver() {
        mAudioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                RECORDER_RATE,
                RECORDER_CHANNEL_OUT,
                RECORDER_AUDIO_ENCODING,
                RECEIVE_BUFFER_SIZE * 2,
                AudioTrack.MODE_STREAM);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mAudioTrack.setVolume(0.9f);
        } else {
            mAudioTrack.setStereoVolume(0.9f, 0.9f);
        }
        mAudioTrack.play();

        buffer = new byte[RECEIVE_BUFFER_SIZE];

        try {
            mReceivingSocket = new DatagramSocket(RECEIVE_PORT);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        isReceiving = true;
        while (isReceiving) {
            try {
                mReceivingSocket.receive(packet);
                mAudioTrack.write(packet.getData(), 0, packet.getLength());
                mAudioTrack.flush();
            } catch (IOException e) {
                Log.e("VR", "IOException");
                isReceiving = false;
                break;
            } catch (Exception e) {
                e.printStackTrace();
                isReceiving = false;
                break;
            }
        }
        if (mReceivingSocket != null) {
            mReceivingSocket.disconnect();
            mReceivingSocket.close();
        }
        mAudioTrack.stop();
        mAudioTrack.release();
    }

    private void startRecording() {

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setParameters("noise_suppression=auto");

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
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
                writeAudioData();
            }
        });
        mRecordingThread.start();
    }

    private void writeAudioData() {
        byteData = new byte[RECORD_BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(byteData, byteData.length);

        while (isRecording) {
            mAudioRecord.read(byteData, 0, byteData.length);

            for (int i = 0; i < mAddresses.size(); i++) {
                try {
                    packet.setAddress(mAddresses.get(i));
                    packet.setData(byteData);
                    packet.setLength(byteData.length);
                    packet.setPort(Configuration.RECEIVE_PORT);
                    mSendingSocket.send(packet);
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

    @Override
    protected void onPause() {
        super.onPause();
        isReceiving = false;
        buffer = null;

        if (mReceivingThread != null) {
            mReceivingThread.interrupt();
            mReceivingThread = null;
        }
        if (mReceivingSocket != null) {
            mReceivingSocket.disconnect();
            mReceivingSocket.close();
            mReceivingSocket = null;
        }
        stopRecording();
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