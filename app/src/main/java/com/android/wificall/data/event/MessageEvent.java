package com.android.wificall.data.event;

/**
 * Created by slobodyanuk on 11.07.16.
 */
public class MessageEvent {

    private String name;
    private String msg;

    public MessageEvent(String name, String msg) {
        this.name = name;
        this.msg = msg;
    }

    public String getName() {
        return name;
    }

    public String getMsg() {
        return msg;
    }
}
