package com.android.wificall.router.audio;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Process;
import android.util.Log;

import com.android.wificall.data.Packet;
import com.android.wificall.router.NetworkManager;
import com.android.wificall.router.Sender;
import com.android.wificall.view.activity.CallActivity;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import static com.android.wificall.router.Configuration.RECEIVE_PORT;
import static com.android.wificall.router.Configuration.RECORDER_AUDIO_ENCODING;
import static com.android.wificall.router.Configuration.RECORDER_CHANNEL_OUT;
import static com.android.wificall.router.Configuration.RECORDER_RATE;

/**
 * Created by Serhii Slobodyanuk on 23.08.2016.
 */
public class AudioReader implements Runnable {

    private int RECEIVE_BUFFER_SIZE;

    private AudioTrack mAudioTrack;
    private CallActivity mActivity;
    private DatagramSocket mReceivingSocket = null;
    private boolean isReceiving = false;

    public AudioReader(CallActivity activity, int bufferSize) {
        mActivity = activity;
        RECEIVE_BUFFER_SIZE = bufferSize;
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
        initReceiver();
    }

    public void initReceiver() {

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                RECORDER_RATE,
                RECORDER_CHANNEL_OUT,
                RECORDER_AUDIO_ENCODING,
                RECEIVE_BUFFER_SIZE * 2,
                AudioTrack.MODE_STREAM);

        AudioManager mAudioManager = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 70, 0);
        mAudioManager.setParameters("noise_suppression=auto");

        mAudioTrack.play();

        byte[] buffer = new byte[RECEIVE_BUFFER_SIZE];

        try {
            mReceivingSocket = new DatagramSocket(RECEIVE_PORT);
            buffer = new byte[mReceivingSocket.getReceiveBufferSize() / 10];
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            byte[] routingTable = NetworkManager.serializeRoutingTable();
            Packet ack = new Packet(Packet.TYPE.HELLO_ACK, routingTable, NetworkManager.getSelf().getGroupOwnerMac(), NetworkManager.getSelf()
                    .getMac());
            Sender.queuePacket(ack);
        } catch (Exception e) {
            mActivity.finish();
        }

        final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        isReceiving = true;

        while (isReceiving && !mActivity.isThreadStopped()) {
            try {
                mReceivingSocket.receive(packet);
                mAudioTrack.write(packet.getData(), 0, packet.getLength());
            } catch (IOException e) {
                Log.e("VR", "IOException");
                isReceiving = false;
                mActivity.setThreadStopped(true);
                break;
            } catch (Exception e) {
                e.printStackTrace();
                mActivity.setThreadStopped(true);
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

    public synchronized AudioTrack getAudioTrack(){
        return mAudioTrack;
    }

    public void stopReceiving(){
        this.isReceiving = false;
        if (mReceivingSocket != null) {
            mReceivingSocket.disconnect();
            mReceivingSocket.close();
            mReceivingSocket = null;
        }
    }

}
