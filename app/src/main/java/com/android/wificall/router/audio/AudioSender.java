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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Serhii Slobodyanuk on 02.09.2016.
 */
public class AudioSender implements OnSendAudioListener {

    private static final String TAG = AudioSender.class.getCanonicalName();
    private static List<Address> mAddresses = new ArrayList<>();
    private DatagramSocket mSendingSocket = null;
    private boolean isUpdate = false;
    private DatagramPacket mPacket;

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
            mPacket = new DatagramPacket(new byte[0], 0);

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
            Set<Address> set = new HashSet<>();
            mAddresses.add(address);
            set.addAll(mAddresses);
            mAddresses.clear();
            mAddresses.addAll(set);
            Log.e(TAG, "addAddress: " + mAddresses.size());
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
        for (int i = 0; i < mAddresses.size(); i++) {
            try {
                if (isUpdate) {
                    Log.e("onSend", mAddresses.get(i).getMac());
                    byte[] rtable = NetworkManager.serializeRoutingTable();
                    Packet ack = new Packet(Packet.TYPE.UPDATE_CONNECTION, rtable, mAddresses.get(i).getMac(), NetworkManager.getSelf()
                            .getMac());
                    Sender.queuePacket(ack);
                }
                mPacket.setAddress(mAddresses.get(i).getInetAddress());
                mPacket.setData(data);
                mPacket.setLength(data.length);
                mPacket.setPort(Configuration.RECEIVE_PORT);
                if (mSendingSocket != null) {
                    mSendingSocket.send(mPacket);
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
