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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Address)) return false;

        Address address = (Address) o;

        if (!mInetAddress.equals(address.mInetAddress)) return false;
        return mac.equals(address.mac);

    }

    @Override
    public int hashCode() {
        return mac.hashCode();
    }
}
