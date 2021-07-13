package com.qiniu.android.http.dns;

import com.qiniu.android.dns.Domain;
import com.qiniu.android.dns.IResolver;
import com.qiniu.android.dns.NetworkInfo;
import com.qiniu.android.dns.Record;
import com.qiniu.android.dns.local.Resolver;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DnsCustomServersResolver implements IResolver {

    public ArrayList<CustomServer> customServers = new ArrayList<>();

    public DnsCustomServersResolver(String[] dnsServers, int timeout) {
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
    public Record[] resolve(Domain domain, NetworkInfo info) throws IOException {
        List<CustomServer> servers = getCustomServers();

        Record[] records = null;
        for (CustomServer server : servers) {
            records = server.resolver.resolve(domain, info);
            if (records != null && records.length > 0) {
                server.addReliability();
                break;
            }
        }

        return records;
    }


    public List<CustomServer> getCustomServers() {
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

    public static class CustomServer {

        private int reliability = 0;
        private IResolver resolver;

        CustomServer(InetAddress address, int timeout) {
            this.resolver = new Resolver(address, timeout);
        }

        public void addReliability() {
            reliability += 1;
        }

        boolean isMoreReliabilityThan(CustomServer customServer) {
            return reliability > customServer.reliability;
        }

        @Override
        public String toString() {
            return "{" + "\"" + "reliability" + "\":" + reliability + "}";
        }
    }
}
