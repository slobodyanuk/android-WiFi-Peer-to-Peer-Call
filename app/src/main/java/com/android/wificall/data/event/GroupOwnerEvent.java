package com.android.wificall.data.event;

/**
 * Created by Serhii Slobodyanuk on 23.08.2016.
 */
public class GroupOwnerEvent {

    private boolean groupOwner;

    public GroupOwnerEvent(boolean groupOwner) {
        this.groupOwner = groupOwner;
    }

    public boolean isGroupOwner() {
        return groupOwner;
    }
}
