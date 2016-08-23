package com.android.wificall.router;

import com.android.wificall.data.Packet;
import com.android.wificall.router.tcp.TcpSender;

import java.util.concurrent.ConcurrentLinkedQueue;

public class Sender implements Runnable {

    private static ConcurrentLinkedQueue<Packet> ccl;

    public Sender() {
        if (ccl == null)
            ccl = new ConcurrentLinkedQueue<Packet>();
    }

    public static boolean queuePacket(Packet p) {
        if (ccl == null)
            ccl = new ConcurrentLinkedQueue<Packet>();
        return ccl.add(p);
    }

    @Override
    public void run() {
        TcpSender packetSender = new TcpSender();

        while (!Thread.currentThread().isInterrupted()) {
            while (!ccl.isEmpty()) {
                Packet p = ccl.remove();
                String ip = NetworkManager.getIpForClient(p.getMac());
                packetSender.sendPacket(ip, Configuration.RECEIVE_PORT, p);
            }

        }
    }
}
