package com.android.wificall.data.event;

/**
 * Created by matviy on 16.09.16.
 */
public class WifiP2PStateChangedEvent {

    private boolean isEnabled;

    public WifiP2PStateChangedEvent(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }
}
