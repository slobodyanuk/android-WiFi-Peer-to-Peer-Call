package com.android.wificall.data;

/**
 * Created by slobodyanuk on 11.07.16.
 */
public class Packet {

    public enum TYPE {
        HELLO, HELLO_ACK, BYE, MESSAGE, UPDATE, RECORDING_AVAILABLE
    }

    private byte[] data;
    private Packet.TYPE type;
    private String receiverMac;
    private String senderMac;
    private String senderIP;
    private int ttl;

    public Packet(Packet.TYPE type, byte[] extraData, String receiverMac, String senderMac) {
        this.setData(extraData);
        this.setType(type);
        this.receiverMac = receiverMac;
        this.setTtl(3);
        if (receiverMac == null)
            this.receiverMac = "00:00:00:00:00:00";
        this.senderMac = senderMac;
    }

    public Packet(TYPE type2, byte[] eData, String receivermac, String senderMac, int timetolive) {
        this.setData(eData);
        this.setType(type2);
        this.receiverMac = receivermac;
        if (receiverMac == null)
            this.receiverMac = "00:00:00:00:00:00";
        this.senderMac = senderMac;
        this.ttl = timetolive;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public TYPE getType() {
        return type;
    }

    public void setType(TYPE type) {
        this.type = type;
    }

    public String getMac() {
        return receiverMac;
    }

    public void setMac(String receiverMac) {
        this.receiverMac = receiverMac;
    }

    public String getSenderMac() {
        return senderMac;
    }

    public void setSenderMac(String senderMac) {
        this.senderMac = senderMac;
    }

    public String getSenderIP() {
        return senderIP;
    }

    public void setSenderIP(String senderIP) {
        this.senderIP = senderIP;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public static byte[] getMacAsBytes(String maca) {
        String[] mac = maca.split(":");
        byte[] macAddress = new byte[6];
        for (int i = 0; i < mac.length; i++) {
            macAddress[i] = Integer.decode("0x" + mac[i]).byteValue();
        }
        return macAddress;
    }

    public static String getMacBytesAsString(byte[] data, int startOffset) {
        StringBuilder sb = new StringBuilder(18);
        for (int i = startOffset; i < startOffset + 6; i++) {
            byte b = data[i];
            if (sb.length() > 0)
                sb.append(':');
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public byte[] serialize() {

        byte[] serialized = new byte[1 + data.length + 13];
        serialized[0] = (byte) type.ordinal();
        serialized[1] = (byte) ttl;

        byte[] mac = getMacAsBytes(this.receiverMac);

        System.arraycopy(mac, 0, serialized, 2, 6);

        mac = getMacAsBytes(this.senderMac);

        System.arraycopy(mac, 0, serialized, 0, 11);
        System.arraycopy(mac, 0, serialized, 8, 6);
        System.arraycopy(data, 0, serialized, 14, serialized.length - 14);

        return serialized;
    }

    public static Packet deserialize(byte[] inputData) {
        Packet.TYPE type = TYPE.values()[(int) inputData[0]];

        byte[] data = new byte[inputData.length - 14];
        int timetolive = (int) inputData[1];
        String mac = getMacBytesAsString(inputData, 2);
        String receivermac = getMacBytesAsString(inputData, 8);

        System.arraycopy(inputData, 14, data, 0, inputData.length - 14);
        return new Packet(type, data, mac, receivermac, timetolive);
    }

    @Override
    public String toString() {
        return "Type" + getType().toString() + "receiver:" + getMac() + "sender:" + getSenderMac();
    }
}
