package com.android.wificall.data.audio;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.android.wificall.data.Packet;
import com.android.wificall.router.NetworkManager;
import com.android.wificall.router.Sender;
import com.android.wificall.router.audio.AudioReceiver;
import com.android.wificall.router.audio.OnReceiveAudioListener;
import com.android.wificall.util.RetryExecution;
import com.android.wificall.view.activity.CallActivity;

import io.reactivex.FlowableEmitter;

import static com.android.wificall.router.Configuration.RECORDER_AUDIO_ENCODING;
import static com.android.wificall.router.Configuration.RECORDER_CHANNEL_OUT;
import static com.android.wificall.router.Configuration.RECORDER_RATE;

/**
 * Created by Serhii Slobodyanuk on 23.08.2016.
 */
public class AudioReader implements OnReceiveAudioListener {
    private static final String TAG = AudioReader.class.getCanonicalName();
    private int RECEIVE_BUFFER_SIZE;

    private AudioTrack mAudioTrack;
    private CallActivity mActivity;
    private AudioReceiver mAudioReceiver;
    private FlowableEmitter mSubscriber;
    private RetryExecution mRetryExecutionListener;

    public AudioReader(CallActivity mActivity, int receive_buffer_size) {
        this.mActivity = mActivity;
        RECEIVE_BUFFER_SIZE = receive_buffer_size;
        mAudioReceiver = new  AudioReceiver(mActivity, RECEIVE_BUFFER_SIZE, this);
        mRetryExecutionListener = mActivity;
    }

    public void setSubscriber(FlowableEmitter subscriber){
        this.mSubscriber = subscriber;
    }

    public void execute() {

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                RECORDER_RATE,
                RECORDER_CHANNEL_OUT,
                RECORDER_AUDIO_ENCODING,
                RECEIVE_BUFFER_SIZE * 2,
                AudioTrack.MODE_STREAM);

        mAudioTrack.play();
        mAudioReceiver.receiveData(mSubscriber);

    }

    @Override
    public void onReceiveData(byte[] data, int length) {
        if (mAudioTrack != null) {
            mAudioTrack.write(data, 0, length);
        }
    }

    public void onReleaseTrack() {
        Log.e(TAG, "onReleaseTrack");
        byte[] rtable = NetworkManager.serializeRoutingTable();
        Packet ack = new Packet(Packet.TYPE.HELLO_ACK, rtable, NetworkManager.getSelf().getGroupOwnerMac(), NetworkManager.getSelf()
                .getMac());
        Sender.queuePacket(ack);

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

    @Override
    public void onUpdateSubscriber() {
        Log.e(TAG, "onUpdateSubscriber");
        onReleaseTrack();
        stop();
        mRetryExecutionListener.onRetryExecution();
    }


    public void stop() {
        if (mSubscriber != null && mAudioReceiver != null) {
            Log.e(TAG, "stop: audio reader");
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
            mSubscriber.onComplete();
            mAudioReceiver.stopReceiving();
        }
    }
}
