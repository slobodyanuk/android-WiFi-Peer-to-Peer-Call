package com.android.wificall.router.reactive;

import android.util.Log;

import com.android.wificall.data.audio.AudioReader;
import com.android.wificall.view.activity.CallActivity;

import io.reactivex.FlowableEmitter;


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


    public void updateReceiver() {
        if (mAudioReader != null) {
            mAudioReader.onUpdateSubscriber();
        }
    }

    public void stop() {
        if (mAudioReader != null) {
            mAudioReader.stop();
        }
    }

    public AudioReader initAudioReader(){
        return mAudioReader = new AudioReader(mActivity, RECEIVE_BUFFER_SIZE);
    }

    @Override
    protected void executeTask(FlowableEmitter subscribe) {
        Log.w("tag exec", "executeTask: " + Thread.currentThread());
        mAudioReader.setSubscriber(subscribe);
        mAudioReader.execute();
    }
}
