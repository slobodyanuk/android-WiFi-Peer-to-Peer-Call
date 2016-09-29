package com.android.wificall.data.event;

/**
 * Created by Serhii Slobodyanuk on 29.09.2016.
 */
public class VolumeEvent {

    private int volume;

    public VolumeEvent(int volume) {
        this.volume = volume;
    }

    public int getVolume() {
        return volume;
    }
}
