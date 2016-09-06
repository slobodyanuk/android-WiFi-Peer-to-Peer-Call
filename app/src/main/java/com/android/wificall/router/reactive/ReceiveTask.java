package com.android.wificall.router.reactive;

import com.android.wificall.data.audio.AudioReader;
import com.android.wificall.view.activity.CallActivity;

import rx.Subscriber;

/**
 * Created by Serhii Slobodyanuk on 05.09.2016.
 */
public class ReceiveTask extends BaseTask {

    private int RECEIVE_BUFFER_SIZE;
    private CallActivity mActivity;
    private AudioReader mAudioReader;

    public ReceiveTask(int RECEIVE_BUFFER_SIZE, CallActivity mActivity) {
        this.RECEIVE_BUFFER_SIZE = RECEIVE_BUFFER_SIZE;
        this.mActivity = mActivity;
    }

    @Override
    protected void executeTask(Subscriber subscriber) {
        mAudioReader = new AudioReader(mActivity, RECEIVE_BUFFER_SIZE, subscriber);
        mAudioReader.execute();
    }

    public void updateReceiver() {
        if (mAudioReader != null) {
            mAudioReader.onReleaseTrack();
        }
    }

    public void stop() {
        if (mAudioReader != null) {
            mAudioReader.stop();
        }
    }
}
