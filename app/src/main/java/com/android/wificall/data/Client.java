package com.android.wificall.data;

/**
 * Created by slobodyanuk on 11.07.16.
 */
public class Client {

    private String mac;
    private String name;
    private String groupOwnerMac;
    private String ip;
    private boolean isDirectLink;

    public Client(String mac_address, String ip, String name, String groupOwner) {
        this.setMac(mac_address);
        this.setName(name);
        this.setIp(ip);
        this.setGroupOwnerMac(groupOwner);
        this.isDirectLink = true;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroupOwnerMac() {
        return groupOwnerMac;
    }

    public void setGroupOwnerMac(String groupOwnerMac) {
        this.groupOwnerMac = groupOwnerMac;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public boolean isDirectLink() {
        return isDirectLink;
    }

    public void setDirectLink(boolean directLink) {
        isDirectLink = directLink;
    }

    @Override
    public String toString() {
        return getIp() + "," + getMac() + "," + getName() + "," + getGroupOwnerMac();
    }

    public static Client fromString(String serialized) {
        String[] divided = serialized.split(",");
        return new Client(divided[1], divided[0], divided[2], divided[3]);
    }

}
