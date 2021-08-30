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

public class UdpDns implements Dns {
    private IResolver udpResolver;

    public UdpDns(int timeout) {
        String[] udpServers = GlobalConfiguration.getInstance().udpDnsServers;
        if (udpServers != null && udpServers.length > 0) {
            udpResolver = new DnsUdpResolver(udpServers, Record.TYPE_A, timeout);
        }
    }

    @Override
    public List<IDnsNetworkAddress> lookup(String hostname) throws UnknownHostException {
        if (!GlobalConfiguration.getInstance().udpDnsEnable) {
            return null;
        }

        if (udpResolver == null) {
            throw new UnknownHostException("resolver server is invalid");
        }

        Record[] records = null;
        try {
            records = udpResolver.resolve(new Domain(hostname), null);
        } catch (IOException ignore) {
            throw new UnknownHostException(ignore.toString());
        }

        if (records == null || records.length == 0) {
            return null;
        }

        ArrayList<IDnsNetworkAddress> addressList = new ArrayList<>();
        for (Record record : records) {
            String source = DnsSource.Udp + ":<" + record.server + ">";
            DnsNetworkAddress address = new DnsNetworkAddress(hostname, record.value, record.timeStamp, source, record.timeStamp);
            addressList.add(address);
        }

        return addressList;
    }
}
