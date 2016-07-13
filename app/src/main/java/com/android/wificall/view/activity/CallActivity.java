package com.android.wificall.view.activity;

import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.android.wificall.R;
import com.android.wificall.data.Client;
import com.android.wificall.data.Packet;
import com.android.wificall.router.Configuration;
import com.android.wificall.router.NetworkManager;
import com.android.wificall.router.Sender;
import com.android.wificall.router.broadcast.WifiDirectBroadcastReceiver;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import butterknife.BindView;
import butterknife.OnClick;

import static com.android.wificall.router.Configuration.RECEIVE_PORT;
import static com.android.wificall.router.Configuration.RECEIVE_VOICE_PORT;
import static com.android.wificall.router.Configuration.RECORDER_AUDIO_ENCODING;
import static com.android.wificall.router.Configuration.RECORDER_CHANNEL_IN;
import static com.android.wificall.router.Configuration.RECORDER_CHANNEL_OUT;
import static com.android.wificall.router.Configuration.RECORDER_RATE;

public class CallActivity extends BaseActivity {

    @BindView(R.id.start)
    Button mStartButton;
    @BindView(R.id.stop)
    Button mStopButton;

    private DatagramSocket mDatagramSocket = null;
    private static AudioTrack mAudioTrack;
    private AudioRecord mAudioRecord= null;

    private Thread mRecordingThread = null;
    private Thread mReceivingThread = null;

    private boolean isRecording = false;
    private int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024

    private int BytesPerElement = 2; // 2 bytes in 16bit format
    private short shortData[];
    private byte byteData[];
    int minBufSize;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!WifiDirectActivity.isGroupOwner) {
            mStartButton.setVisibility(View.GONE);
            mStopButton.setVisibility(View.GONE);
        }
        minBufSize = AudioTrack.getMinBufferSize(RECORDER_RATE, RECORDER_CHANNEL_OUT, RECORDER_AUDIO_ENCODING);

        enableButtons(false);

        mReceivingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (Client c : NetworkManager.routingTable.values()) {
                    if (c.getMac().equals(NetworkManager.getSelf().getMac())) {
                        Log.e("call", "self continue");
                        continue;
                    }
                    Runtime runtime = Runtime.getRuntime();
                    try {
                        // Ping 10 times at 169.254.169.168
                        runtime.exec("/system/bin/ping -c 10 " + c.getIp());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                        initReceiver();
                }
            }
        });

        mReceivingThread.start();
    }

    @OnClick(R.id.start)
    public void onStartClick(){
        enableButtons(true);
        startRecording();
    }

    @OnClick(R.id.stop)
    public void onStopClick(){
        enableButtons(false);
        stopRecording();
    }

    public void initReceiver() {
        Log.e("call", "initReceiver");

        mAudioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                RECORDER_RATE,
                RECORDER_CHANNEL_OUT,
                RECORDER_AUDIO_ENCODING,
                minBufSize,
                AudioTrack.MODE_STREAM);

        mAudioTrack.play();

        byte[] buffer = new byte[minBufSize];

        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(RECEIVE_PORT);
        } catch (SocketException e) {
                        e.printStackTrace();
        }
        while (true) {
            try {

                final DatagramPacket packet = new DatagramPacket(buffer, minBufSize);
                Log.e("Call receive", String.valueOf(packet.getData()));
                socket.receive(packet);
                CallActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAudioTrack.write(packet.getData(), 0, packet.getLength());
                    }
                });
            } catch (IOException e) {
                Log.e("VR", "IOException");
                break;
            }
        }
        socket.disconnect();
        socket.close();
        mAudioTrack.stop();
        mAudioTrack.flush();
        mAudioTrack.release();
    }

    private void startRecording() {

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_RATE, RECORDER_CHANNEL_IN,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        mAudioRecord.startRecording();
        isRecording = true;

        try {
            mDatagramSocket = new DatagramSocket();
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
        shortData = new short[BufferElements2Rec];

        while (isRecording) {
//            CallActivity.this.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
                    mAudioRecord.read(shortData, 0, BufferElements2Rec);
//                }
//            });
            byteData = short2byte(shortData);
            for (Client c : NetworkManager.routingTable.values()) {
                if (c.getMac().equals(NetworkManager.getSelf().getMac()))
                    continue;
                try {

                    DatagramPacket packet = new DatagramPacket(byteData, byteData.length, InetAddress.getByName(c.getIp()), Configuration.RECEIVE_PORT);
                    Log.e("Call send", String.valueOf(packet.getData()));

                    mDatagramSocket.send(packet);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

//                Sender.queuePacket(new Packet(Packet.TYPE.VOICE, byteData, c.getMac(),
//                        WifiDirectBroadcastReceiver.MAC));
            }
        }
    }

    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    private void stopRecording() {
        if (null != mAudioRecord) {
            isRecording = false;
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
//            mRecordingThread = null;
        }
//        if (mDatagramSocket != null){
//            mDatagramSocket.disconnect();
//            mDatagramSocket.close();
//        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //stopRecording();
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