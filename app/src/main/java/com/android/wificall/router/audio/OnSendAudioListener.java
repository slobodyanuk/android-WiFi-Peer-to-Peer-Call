package com.android.wificall.router.audio;

/**
 * Created by Serhii Slobodyanuk on 22.09.2016.
 */
public interface OnSendAudioListener {

    void onSendAudioData(byte[] data);

    void onUpdateConnection();

    void onCompleted();

}
