package com.android.wificall.router.reactive;

import com.android.wificall.data.audio.AudioRecorder;
import com.android.wificall.view.activity.CallActivity;

import rx.Subscriber;

/**
 * Created by Serhii Slobodyanuk on 01.09.2016.
 */
public class RecordTask extends BaseTask<byte[]> implements AudioRecorder.OnSendVoice {

    private CallActivity mActivity;
    private int RECORD_BUFFER_SIZE;
    private Subscriber mSubscriber;
    private static AudioRecorder record;

    public RecordTask(CallActivity mActivity, int RECORD_BUFFER_SIZE) {
        this.mActivity = mActivity;
        this.RECORD_BUFFER_SIZE = RECORD_BUFFER_SIZE;
    }

    @Override
    protected void executeTask(Subscriber subscriber) {
        mSubscriber = subscriber;
        record = new AudioRecorder(mActivity, RECORD_BUFFER_SIZE, this);
        record.executeRecording();
    }

    public void stop() {
        if (record != null) {
            record.stopRecording();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSendAudioData(byte[] data) {
        mSubscriber.onNext(data);
    }

    @Override
    public void onCompleted() {
        mSubscriber.onCompleted();
    }
}
