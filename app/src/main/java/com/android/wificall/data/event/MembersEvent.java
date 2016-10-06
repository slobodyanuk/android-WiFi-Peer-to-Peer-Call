package com.android.wificall.data.event;

import com.android.wificall.data.Packet;

/**
 * Created by Serhii Slobodyanuk on 05.10.2016.
 */
public class MembersEvent {

    private Packet p;

    public MembersEvent(Packet p) {
        this.p = p;
    }

    public Packet getPacket() {
        return p;
    }
}
