package com.android.wificall.data.audio;

import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.android.wificall.router.audio.AudioSender;
import com.android.wificall.view.activity.CallActivity;

import rx.Subscriber;

import static com.android.wificall.router.Configuration.RECORDER_AUDIO_ENCODING;
import static com.android.wificall.router.Configuration.RECORDER_CHANNEL_IN;
import static com.android.wificall.router.Configuration.RECORDER_RATE;

/**
 * Created by Serhii Slobodyanuk on 23.08.2016.
 */
public class AudioRecorder  {

    private int RECORD_BUFFER_SIZE;
    private boolean isRecording;
    private CallActivity mActivity;
    private AudioRecord mAudioRecord;
    private OnSendVoice mSendCallback;
    private Subscriber mSubscriber;

    public AudioRecorder(CallActivity mActivity, int RECORD_BUFFER_SIZE, Subscriber subscriber) {
        this.mActivity = mActivity;
        this.RECORD_BUFFER_SIZE = RECORD_BUFFER_SIZE;
        this.mSubscriber = subscriber;
        this.mSendCallback = new AudioSender();
    }

    public void executeRecording() {

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                RECORDER_RATE,
                RECORDER_CHANNEL_IN,
                RECORDER_AUDIO_ENCODING,
                RECORD_BUFFER_SIZE);

        mAudioRecord.startRecording();
        isRecording = true;

        byte[] byteData = new byte[RECORD_BUFFER_SIZE];
        isRecording = true;

        while (isRecording && !mActivity.isThreadStopped()) {
                mAudioRecord.read(byteData, 0, byteData.length);
                mSendCallback.onSendAudioData(byteData);
        }
    }

    public interface OnSendVoice {
        void onSendAudioData(byte[] data);
        void onCompleted();
    }

    public void stopRecording() {
        mSendCallback.onCompleted();
        mSubscriber.onCompleted();
        if (null != mAudioRecord) {
            isRecording = false;
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
    }
}
