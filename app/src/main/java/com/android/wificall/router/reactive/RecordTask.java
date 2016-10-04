package com.android.wificall.router.reactive;

import com.android.wificall.data.audio.AudioRecorder;
import com.android.wificall.view.activity.CallActivity;

import io.reactivex.FlowableEmitter;


/**
 * Created by Serhii Slobodyanuk on 01.09.2016.
 */
public class RecordTask extends BaseTask<byte[]> {

    private CallActivity mActivity;
    private int RECORD_BUFFER_SIZE;
    private AudioRecorder record;

    public RecordTask(CallActivity mActivity, int RECORD_BUFFER_SIZE) {
        this.mActivity = mActivity;
        this.RECORD_BUFFER_SIZE = RECORD_BUFFER_SIZE;
    }

    public void updateRecorder() {
        if (record != null) {
            record.onUpdateSubscriber();
        }
    }

    public void stop() {
        if (record != null) {
            record.stopRecording();
        }
    }

    @Override
    protected void executeTask(FlowableEmitter<byte[]> subscribe) {
        record = new AudioRecorder(mActivity, RECORD_BUFFER_SIZE, subscribe);
        record.executeRecording();
    }
}
