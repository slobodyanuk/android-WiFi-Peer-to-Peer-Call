package com.android.wificall.router;

import com.android.wificall.data.Client;
import com.android.wificall.data.Packet;
import com.android.wificall.data.event.MessageEvent;
import com.android.wificall.data.event.SomebodyJoinedEvent;
import com.android.wificall.data.event.SomebodyLeftEvent;
import com.android.wificall.data.event.UpdateRoomInfoEvent;
import com.android.wificall.router.audio.AudioSender;
import com.android.wificall.router.tcp.TcpReceiver;

import org.greenrobot.eventbus.EventBus;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by slobodyanuk on 11.07.16.
 */
public class Receiver implements Runnable {

    public static boolean running = false;

    public Receiver() {
        running = true;
    }

    public static void somebodyJoined(String smac, String ip) {
        final String msg;
        msg = smac + " has joined.";
        final String name = smac;
        EventBus.getDefault().post(new SomebodyJoinedEvent());

        try {
            AudioSender.addAddress(InetAddress.getByName(ip));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }

    public static void somebodyLeft(String smac, String ip) {
        final String msg;
        msg = smac + " has left.";
        final String name = smac;
        EventBus.getDefault().post(new SomebodyLeftEvent());

        try {
            AudioSender.removeAddress(InetAddress.getByName(ip));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static void updatePeerList() {
        EventBus.getDefault().post(new UpdateRoomInfoEvent());
    }

    @Override
    public void run() {
        ConcurrentLinkedQueue<Packet> packetQueue = new ConcurrentLinkedQueue<Packet>();
        new Thread(new TcpReceiver(Configuration.RECEIVE_PORT, packetQueue)).start();
        Packet p;
        while (true) {
            while (!packetQueue.isEmpty()) {
                p = packetQueue.remove();

                if (p.getType().equals(Packet.TYPE.HELLO)) {
                    for (Client c : NetworkManager.routingTable.values()) {
                        if (c.getMac().equals(NetworkManager.getSelf().getMac()) || c.getMac().equals(p.getSenderMac()))
                            continue;
                        Packet update = new Packet(Packet.TYPE.UPDATE, Packet.getMacAsBytes(p.getSenderMac()), c.getMac(),
                                NetworkManager.getSelf().getMac());
                        Sender.queuePacket(update);
                    }

                    NetworkManager.routingTable.put(p.getSenderMac(),
                            new Client(p.getSenderMac(), p.getSenderIP(), p.getSenderMac(),
                                    NetworkManager.getSelf().getMac()));

                    byte[] rtable = NetworkManager.serializeRoutingTable();

                    Packet ack = new Packet(Packet.TYPE.HELLO_ACK, rtable, p.getSenderMac(), NetworkManager.getSelf()
                            .getMac());
                    Sender.queuePacket(ack);
                    somebodyJoined(p.getSenderMac(), p.getSenderIP());
                    updatePeerList();
                } else {
                    // If you're the intended target for a non hello message
                    if (p.getMac().equals(NetworkManager.getSelf().getMac())) {
                        //if we get a hello ack populate the table
                        if (p.getType().equals(Packet.TYPE.HELLO_ACK)) {
                            NetworkManager.deserializeRoutingTableAndAdd(p.getData());
                            NetworkManager.getSelf().setGroupOwnerMac(p.getSenderMac());
                            somebodyJoined(p.getSenderMac(), p.getSenderIP());
                            updatePeerList();
                        } else if (p.getType().equals(Packet.TYPE.UPDATE)) {
                            //if it's an update, add to the table
                            String emb_mac = Packet.getMacBytesAsString(p.getData(), 0);
                            NetworkManager.routingTable.put(emb_mac,
                                    new Client(emb_mac, p.getSenderIP(), p.getMac(), NetworkManager
                                            .getSelf().getMac()));

                            final String message = emb_mac + " joined the conversation";
                            final String name = p.getSenderMac();
                            EventBus.getDefault().post(new MessageEvent(name, message));
                            updatePeerList();

                        } else if (p.getType().equals(Packet.TYPE.MESSAGE)) {
                            //If it's a message display the message and update the table if they're not there
                            // for whatever reason
                            final String message = p.getSenderMac() + " says:\n" + new String(p.getData());
                            final String msg = new String(p.getData());
                            final String name = p.getSenderMac();

                            if (!NetworkManager.routingTable.contains(p.getSenderMac())) {
                            /*
                             * Update your routing table if for some reason this
							 * isn't in it
							 */
                                NetworkManager.routingTable.put(p.getSenderMac(),
                                        new Client(p.getSenderMac(), p.getSenderIP(), p.getSenderMac(),
                                                NetworkManager.getSelf().getGroupOwnerMac()));
                            }

                            EventBus.getDefault().post(new MessageEvent(name, msg));
                            updatePeerList();
                        } else if (p.getType().equals(Packet.TYPE.BYE)) {
                            NetworkManager.routingTable.remove(p.getSenderMac());
                            somebodyLeft(p.getSenderMac(), p.getSenderIP());
                            updatePeerList();
                        } else {
                            // otherwise forward it if you're not the recipient
                            int ttl = p.getTtl();
                            // Have a ttl so that they don't bounce around forever
                            ttl--;
                            if (ttl > 0) {
                                Sender.queuePacket(p);
                                p.setTtl(ttl);
                            }
                        }
                    }
                }
            }
        }
    }
}
