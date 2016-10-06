package com.android.wificall.util;

import android.app.Activity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.wificall.R;

/**
 * Created by Serhii Slobodyanuk on 28.09.2016.
 */
public class MuteUtil {

    private boolean isSpeaker;
    private Activity mActivity;
    private boolean isMuted;

    public MuteUtil(Activity activity, boolean isSpeaker) {
        this.mActivity = activity;
        this.isSpeaker = isSpeaker;
    }

    public int initMute(boolean mute) {
        isMuted = mute;
        if (isSpeaker) {
            return (isMuted) ? R.drawable.ic_block_microphone : R.drawable.ic_microphone;
        } else {
            return (isMuted) ? R.drawable.ic_mute_voice : R.drawable.ic_allow_voice;
        }
    }

    public int initConversationLogo(TextView textView, ImageButton imageButton){
        imageButton.setVisibility((isMuted) ? View.GONE : View.VISIBLE);
        if (isSpeaker) {
            textView.setText((isMuted)
                    ? mActivity.getString(R.string.record_mute)
                    : mActivity.getString(R.string.record_msg));
            return (isMuted)
                    ? R.drawable.ic_microphone_off
                    : R.drawable.ic_people_conference;
        } else {
            textView.setText((isMuted)
                    ? mActivity.getString(R.string.receive_mute)
                    : mActivity.getString(R.string.receive_msg));
            return (isMuted)
                    ? R.drawable.ic_volume_off
                    : R.drawable.ic_people_conference;
        }
    }

}