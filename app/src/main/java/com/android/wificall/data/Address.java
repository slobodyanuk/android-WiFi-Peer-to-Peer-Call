package com.android.wificall.data;

import java.net.InetAddress;

/**
 * Created by Serhii Slobodyanuk on 04.10.2016.
 */
public class Address {

    private InetAddress mInetAddress;
    private String mac;

    public Address(InetAddress address, String mac) {
        this.mInetAddress = address;
        this.mac = mac;
    }

    public InetAddress getInetAddress() {
        return mInetAddress;
    }

    public String getMac() {
        return mac;
    }
    @Override
    public boolean equals(final Object obj) {
        if (obj == null || obj == this || !(obj instanceof Address))
            return false;

        Address otherCard = (Address) obj;

        return otherCard.mInetAddress == this.mInetAddress && otherCard.mac.equals(this.mac);

    }

}
