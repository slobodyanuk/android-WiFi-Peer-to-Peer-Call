package com.android.wificall.router;

import android.util.Log;

import com.android.wificall.data.Client;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by slobodyanuk on 11.07.16.
 */
public class NetworkManager {

    public static ConcurrentHashMap<String, Client> routingTable = new ConcurrentHashMap<String, Client>();

    private static Client self;

    public static void newClient(Client c) {
        routingTable.put(c.getMac(), c);
    }

    public static void clientGone(String mac) {
            Log.e("Manager", "clientGone: " + mac);
            routingTable.remove(mac);
    }

    public static Client getSelf() {
        return self;
    }

    public static void setSelf(Client self) {
        NetworkManager.self = self;
        newClient(self);
    }

    public static String getIpForClient(Client c) {

        if (self.getGroupOwnerMac() == c.getGroupOwnerMac()) {
            return c.getIp();
        }

        Client go = routingTable.get(c.getGroupOwnerMac());

        // I am the group owner so can propagate
        if (self.getGroupOwnerMac() == self.getMac()) {
            if (self.getGroupOwnerMac() != c.getGroupOwnerMac() && go.isDirectLink()) {
                // not the same group owner, but we have the group owner as a
                // direct link
                return c.getIp();
            } else if (go != null && self.getGroupOwnerMac() != c.getGroupOwnerMac() && !go.isDirectLink()) {
                for(Client client : routingTable.values()){
                    if(client.getGroupOwnerMac().equals(client.getMac())){
                        return client.getIp();
                    }
                }
                //no other group owners, don't know who to send it to
                return "0.0.0.0";
            }
        } else if (go != null) {
            return Configuration.GO_IP;
        }

        //Will drop the packet
        return "0.0.0.0";
    }

    public static byte[] serializeRoutingTable() {
        StringBuilder serialized = new StringBuilder();

        for (Client v : routingTable.values()) {
            serialized.append(v.toString());
            serialized.append("\n");
        }

        return serialized.toString().getBytes();
    }

    public static void deserializeRoutingTableAndAdd(byte[] rtable) {
        String rstring = new String(rtable);

        String[] div = rstring.split("\n");
        for (String s : div) {
            Client a = Client.fromString(s);
            routingTable.put(a.getMac(), a);
        }
    }

    public static String getIpForClient(String mac) {
        Client c = routingTable.get(mac);
        if (c == null) {
            return Configuration.GO_IP;
        }
        return getIpForClient(c);
    }
}
