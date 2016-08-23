package com.android.wificall.router.audio;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;

import com.android.wificall.data.Client;
import com.android.wificall.router.Configuration;
import com.android.wificall.router.NetworkManager;
import com.android.wificall.view.activity.CallActivity;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static com.android.wificall.router.Configuration.RECORDER_AUDIO_ENCODING;
import static com.android.wificall.router.Configuration.RECORDER_CHANNEL_IN;
import static com.android.wificall.router.Configuration.RECORDER_RATE;

/**
 * Created by Serhii Slobodyanuk on 23.08.2016.
 */
public class AudioRecorder implements Runnable{

    private int RECORD_BUFFER_SIZE;
    private boolean isRecording;
    private CallActivity mActivity;
    private AudioRecord mAudioRecord;
    private ArrayList<InetAddress> mAddresses = new ArrayList<>();
    private DatagramSocket mSendingSocket = null;

    public AudioRecorder(CallActivity mActivity, int RECORD_BUFFER_SIZE) {
        this.mActivity = mActivity;
        this.RECORD_BUFFER_SIZE = RECORD_BUFFER_SIZE;
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
        writeAudioData();
    }

    private void writeAudioData() {

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                RECORDER_RATE,
                RECORDER_CHANNEL_IN,
                RECORDER_AUDIO_ENCODING,
                RECORD_BUFFER_SIZE);

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

        byte[] byteData = new byte[RECORD_BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(byteData, byteData.length);
        isRecording = true;

        while (isRecording && !mActivity.isThreadStopped()) {
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

    public void setAddress(InetAddress address){
        if (mAddresses != null) {
            mAddresses.add(address);
            Set<InetAddress> hs = new HashSet<>();
            hs.addAll(mAddresses);
            mAddresses.clear();
            mAddresses.addAll(hs);
            hs.clear();
        }
    }

    public void removeAddress(InetAddress address){
        if (mAddresses != null) {
            for (int i = 0; i < mAddresses.size(); i++) {
                if (mAddresses.get(i).equals(address)) {
                    mAddresses.remove(i);
                }
            }
        }
    }

    public void stopRecording(){
        if (null != mAudioRecord) {
            isRecording = false;
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }

        if (mSendingSocket != null) {
            mSendingSocket.disconnect();
            mSendingSocket.close();
            mSendingSocket = null;
        }
    }
}
