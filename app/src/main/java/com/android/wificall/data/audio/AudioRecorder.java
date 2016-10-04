package com.android.wificall.data.audio;

import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.android.wificall.util.RetryExecution;
import com.android.wificall.view.activity.CallActivity;

import io.reactivex.FlowableEmitter;

import static com.android.wificall.router.Configuration.RECORDER_AUDIO_ENCODING;
import static com.android.wificall.router.Configuration.RECORDER_CHANNEL_IN;
import static com.android.wificall.router.Configuration.RECORDER_RATE;

/**
 * Created by Serhii Slobodyanuk on 23.08.2016.
 */
public class AudioRecorder {

    private int RECORD_BUFFER_SIZE;
    private boolean isRecording;
    private CallActivity mActivity;
    private AudioRecord mAudioRecord;
    private FlowableEmitter<byte[]> mSubscriber;
    private RetryExecution mRetryExecutionListener;

    public AudioRecorder(CallActivity mActivity, int RECORD_BUFFER_SIZE, FlowableEmitter<byte[]> subscriber) {
        this.mActivity = mActivity;
        this.mRetryExecutionListener = mActivity;
        this.RECORD_BUFFER_SIZE = RECORD_BUFFER_SIZE;
        this.mSubscriber = subscriber;
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
            mSubscriber.onNext(byteData);
        }
    }

    public void onUpdateSubscriber() {
        stopRecording();
        mRetryExecutionListener.onRetryExecution();
    }

    public void stopRecording() {
        mSubscriber.onComplete();
        if (mAudioRecord != null) {
            isRecording = false;
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
    }
}
