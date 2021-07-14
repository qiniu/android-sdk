package com.qiniu.android.http.dns;

import com.qiniu.android.dns.Domain;
import com.qiniu.android.dns.IResolver;
import com.qiniu.android.dns.Record;
import com.qiniu.android.dns.local.Resolver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class ServersDns implements Dns {

    private ArrayList<CustomServer> customServers = new ArrayList<>();

    public ServersDns(String[] dnsServers, int timeout) {
        if (timeout < 0 || timeout > 20) {
            timeout = 2;
        }

        for (String dnsServer : dnsServers) {
            try {
                InetAddress address = InetAddress.getByName(dnsServer);
                customServers.add(new CustomServer(address, timeout));
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    public List<IDnsNetworkAddress> lookup(String hostname) throws UnknownHostException {
        if (customServers.size() == 0) {
            return null;
        }

        List<CustomServer> servers = getCustomServers();

        String serverIP = null;
        Record[] records = null;
        for (CustomServer server : servers) {
            try {
                records = server.resolver.resolve(new Domain(hostname), null);
            } catch (IOException ignore) {
            }

            if (records != null && records.length > 0) {
                serverIP = server.serverIP;
                server.addReliability();
                break;
            }
        }

        ArrayList<IDnsNetworkAddress> addressList = new ArrayList<>();
        for (Record record : records) {
            DnsNetworkAddress address = new DnsNetworkAddress(hostname, record.value, record.timeStamp, serverIP, (new Date()).getTime());
            addressList.add(address);
        }

        return addressList;
    }

    private List<CustomServer> getCustomServers() {
        List<CustomServer> servers = (List<CustomServer>) customServers.clone();
        Comparator<CustomServer> comparator = new Comparator<CustomServer>() {
            @Override
            public int compare(CustomServer o1, CustomServer o2) {
                if (o1.isMoreReliabilityThan(o2)) {
                    return -1;
                } else {
                    return 1;
                }
            }
        };

        Collections.sort(servers, comparator);
        return servers;
    }

    private static class CustomServer {

        private String serverIP = null;
        private int reliability = 0;
        private IResolver resolver;

        CustomServer(InetAddress address, int timeout) {
            this.resolver = new Resolver(address, timeout);
            this.serverIP = address.getHostName();
        }

        private void addReliability() {
            reliability += 1;
        }

        private boolean isMoreReliabilityThan(CustomServer customServer) {
            return reliability > customServer.reliability;
        }

        @Override
        public String toString() {
            return "{" + "\"" + "reliability" + "\":" + reliability + "}";
        }
    }
}
