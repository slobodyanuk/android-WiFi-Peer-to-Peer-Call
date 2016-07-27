package com.android.wificall.data.event;

/**
 * Created by slobodyanuk on 11.07.16.
 */
public class ActivityEvent {

    private String msg;
    private String mac;
    private boolean join;

    public ActivityEvent(String msg) {
        this.msg = msg;
    }

    public ActivityEvent(String msg, String mac,  boolean join) {
        this.msg = msg;
        this.mac = mac;
        this.join = join;
    }

    public String getMac() {
        return mac;
    }

    public String getMsg() {
        return msg;
    }

    public boolean isJoin() {
        return join;
    }
}
