package com.android.wificall.util;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;

import com.android.wificall.data.event.VolumeEvent;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Serhii Slobodyanuk on 29.09.2016.
 */
public class SettingsContentObserver extends ContentObserver {
    private static final String TAG = SettingsContentObserver.class.getCanonicalName();
    int previousVolume;
    Context context;

    public SettingsContentObserver(Context c, Handler handler) {
        super(handler);
        context = c;
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        previousVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    @Override
    public boolean deliverSelfNotifications() {
        return super.deliverSelfNotifications();
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);

        int delta = previousVolume - currentVolume;

        if (delta > 0) {
            previousVolume = currentVolume;
            EventBus.getDefault().post(new VolumeEvent(previousVolume));
        } else if (delta < 0) {
            previousVolume = currentVolume;
            EventBus.getDefault().post(new VolumeEvent(previousVolume));
        }
    }
}
