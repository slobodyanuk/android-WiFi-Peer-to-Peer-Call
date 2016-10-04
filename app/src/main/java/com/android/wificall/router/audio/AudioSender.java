package com.android.wificall.router.audio;

import android.util.Log;

import com.android.wificall.data.Address;
import com.android.wificall.data.Client;
import com.android.wificall.data.Packet;
import com.android.wificall.router.Configuration;
import com.android.wificall.router.NetworkManager;
import com.android.wificall.router.Sender;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Created by Serhii Slobodyanuk on 02.09.2016.
 */
public class AudioSender implements OnSendAudioListener {

    private static final String TAG = AudioSender.class.getCanonicalName();
    private static ArrayList<Address> mAddresses = new ArrayList<>();
    private DatagramSocket mSendingSocket = null;
    private boolean isUpdate = false;

    public AudioSender() {
        initSocket();
    }

    private void initSocket() {
        for (Client c : NetworkManager.routingTable.values()) {
            if (c.getMac().equals(NetworkManager.getSelf().getMac()))
                continue;
            try {
                addAddress(new Address(InetAddress.getByName(c.getIp()), c.getMac()));
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
    }

    public void releaseSocket() {
        Log.e(TAG, "releaseSocket: ");
        if (mSendingSocket != null) {
            mSendingSocket.disconnect();
            mSendingSocket.close();
            mSendingSocket = null;
        }
    }

    public static void addAddress(Address address) {
        if (mAddresses != null) {
            if (mAddresses.size() == 0) {
                mAddresses.add(address);
            }
            for (Address tmp : mAddresses) {
                if (!address.getMac().equals(tmp.getMac())) {
                    mAddresses.add(address);
                    Log.e("addAddress", String.valueOf(mAddresses.size()));
                }
            }
        }
    }

    public static void removeAddress(Address address) {
        if (mAddresses != null) {
            for (int i = 0; i < mAddresses.size(); i++) {
                if (mAddresses.get(i).equals(address)) {
                    mAddresses.remove(i);
                }
            }
        }
    }

    @Override
    public void onSendAudioData(byte[] data) {
        DatagramPacket packet = new DatagramPacket(data, data.length);
        for (int i = 0; i < mAddresses.size(); i++) {
            try {
                if (isUpdate) {
                    Log.e("onSend", mAddresses.get(i).getMac());
                    byte[] rtable = NetworkManager.serializeRoutingTable();
                    Packet ack = new Packet(Packet.TYPE.UPDATE_CONNECTION, rtable, mAddresses.get(i).getMac(), NetworkManager.getSelf()
                            .getMac());
                    Sender.queuePacket(ack);
                }
                packet.setAddress(mAddresses.get(i).getInetAddress());
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
        isUpdate = false;

    }

    @Override
    public void onUpdateConnection() {
        isUpdate = true;
    }

    @Override
    public void onCompleted() {
        releaseSocket();
    }
}
