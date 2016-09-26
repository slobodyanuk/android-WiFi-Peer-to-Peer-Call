package com.android.wificall.router.audio;

/**
 * Created by Serhii Slobodyanuk on 22.09.2016.
 */
public interface OnReceiveAudioListener {

    void onReceiveData(byte[] data, int length);

    void onUpdateSubscriber();

}
