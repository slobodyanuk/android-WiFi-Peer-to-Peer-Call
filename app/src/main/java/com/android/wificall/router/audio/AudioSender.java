package com.android.wificall.router.audio;

import com.android.wificall.data.Client;
import com.android.wificall.router.Configuration;
import com.android.wificall.router.NetworkManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Serhii Slobodyanuk on 02.09.2016.
 */
public class AudioSender {

    private static ArrayList<InetAddress> mAddresses = new ArrayList<>();
    private DatagramSocket mSendingSocket = null;

    public void sendAudio(byte[] data) {

        for (Client c : NetworkManager.routingTable.values()) {
            if (c.getMac().equals(NetworkManager.getSelf().getMac()))
                continue;
            try {
                mAddresses.add(InetAddress.getByName(c.getIp()));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        try {
            mSendingSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }

        DatagramPacket packet = new DatagramPacket(data, data.length);
        for (int i = 0; i < mAddresses.size(); i++) {
            try {
                packet.setAddress(mAddresses.get(i));
                packet.setData(data);
                packet.setLength(data.length);
                packet.setPort(Configuration.RECEIVE_PORT);
                if (mSendingSocket != null) {
                    mSendingSocket.send(packet);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void releaseSocket(){
        if (mSendingSocket != null) {
            mSendingSocket.disconnect();
            mSendingSocket.close();
            mSendingSocket = null;
        }
    }

    public static void addAddress(InetAddress address) {
        if (mAddresses != null) {
            mAddresses.add(address);
            Set<InetAddress> hs = new HashSet<>();
            hs.addAll(mAddresses);
            mAddresses.clear();
            mAddresses.addAll(hs);
            hs.clear();
        }
    }

    public static void removeAddress(InetAddress address) {
        if (mAddresses != null) {
            for (int i = 0; i < mAddresses.size(); i++) {
                if (mAddresses.get(i).equals(address)) {
                    mAddresses.remove(i);
                }
            }
        }
    }
}
