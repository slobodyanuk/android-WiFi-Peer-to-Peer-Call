package com.android.wificall.router.tcp;

import com.android.wificall.data.Packet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by slobodyanuk on 11.07.16.
 */
public class TcpReceiver implements Runnable {

    private ServerSocket serverSocket;
    private ConcurrentLinkedQueue<Packet> packetQueue;

    public TcpReceiver(int port, ConcurrentLinkedQueue<Packet> packetQueue) {
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("Server socket on port " + port + " could not be created. ");
            e.printStackTrace();
        }
        this.packetQueue = packetQueue;
    }

    @Override
    public void run() {
        Socket socket;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                socket = this.serverSocket.accept();

                InputStream in = socket.getInputStream();
                ByteArrayOutputStream os = new ByteArrayOutputStream();

                byte[] buf = new byte[1024];
                while (true) {
                    int n = in.read(buf);
                    if (n < 0)
                        break;
                    os.write(buf, 0, n);

                }

                byte trimmedBytes[] = os.toByteArray();
                Packet p = Packet.deserialize(trimmedBytes);
                p.setSenderIP(socket.getInetAddress().getHostAddress());
                this.packetQueue.add(p);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
