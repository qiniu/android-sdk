package com.qiniu.android.http.dns;

import com.qiniu.android.dns.Domain;
import com.qiniu.android.dns.IResolver;
import com.qiniu.android.dns.Record;
import com.qiniu.android.dns.dns.DnsUdpResolver;
import com.qiniu.android.storage.GlobalConfiguration;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class UdpDns extends BaseDns implements Dns {
    private IResolver udpIpv4Resolver;
    private IResolver udpIpv6Resolver;

    public UdpDns(int timeout) {
        String[] udpIpv4Servers = GlobalConfiguration.getInstance().getUdpDnsIpv4Servers();
        if (udpIpv4Servers != null && udpIpv4Servers.length > 0) {
            udpIpv4Resolver = new DnsUdpResolver(udpIpv4Servers, Record.TYPE_A, timeout, executor);
        }

        String[] udpIpv6Servers = GlobalConfiguration.getInstance().getUdpDnsIpv6Servers();
        if (udpIpv6Servers != null && udpIpv6Servers.length > 0) {
            udpIpv6Resolver = new DnsUdpResolver(udpIpv6Servers, Record.TYPE_A, timeout, executor);
        }
    }

    @Override
    public List<IDnsNetworkAddress> lookup(String hostname) throws UnknownHostException {
        if (!GlobalConfiguration.getInstance().udpDnsEnable) {
            return null;
        }

        if (udpIpv4Resolver == null && udpIpv6Resolver == null) {
            throw new UnknownHostException("resolver server is invalid");
        }

        Record[] records = null;
        if (udpIpv4Resolver != null) {
            try {
                records = udpIpv4Resolver.resolve(new Domain(hostname), null);
            } catch (IOException ignore) {
            }
        }

        if ((records == null || records.length == 0) && udpIpv6Resolver != null) {
            try {
                records = udpIpv6Resolver.resolve(new Domain(hostname), null);
            } catch (IOException ignore) {
            }
        }

        if (records == null || records.length == 0) {
            return null;
        }

        ArrayList<IDnsNetworkAddress> addressList = new ArrayList<>();
        for (Record record : records) {
            if (record.isA() || record.isAAAA()) {
                String source = DnsSource.Udp + ":<" + record.server + ">";
                DnsNetworkAddress address = new DnsNetworkAddress(hostname, record.value, record.timeStamp, source, record.timeStamp);
                addressList.add(address);
            }
        }

        return addressList;
    }
}
