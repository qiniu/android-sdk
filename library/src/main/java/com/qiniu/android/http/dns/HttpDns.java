package com.qiniu.android.http.dns;

import com.qiniu.android.dns.Domain;
import com.qiniu.android.dns.IResolver;
import com.qiniu.android.dns.Record;
import com.qiniu.android.dns.dns.DohResolver;
import com.qiniu.android.storage.GlobalConfiguration;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class HttpDns extends BaseDns implements Dns {

    private IResolver httpIpv4Resolver;
    private IResolver httpIpv6Resolver;

    public HttpDns(int timeout) {
        String[] dohIpv4Servers = GlobalConfiguration.getInstance().getDohIpv4Servers();
        if (dohIpv4Servers != null && dohIpv4Servers.length > 0) {
            httpIpv4Resolver = new DohResolver(dohIpv4Servers, Record.TYPE_A, timeout, executor);
        }

        String[] dohIpv6Servers = GlobalConfiguration.getInstance().getDohIpv6Servers();
        if (dohIpv6Servers != null && dohIpv6Servers.length > 0) {
            httpIpv6Resolver = new DohResolver(dohIpv6Servers, Record.TYPE_A, timeout, executor);
        }
    }

    @Override
    public List<IDnsNetworkAddress> lookup(String hostname) throws UnknownHostException {
        if (!GlobalConfiguration.getInstance().dohEnable) {
            return null;
        }

        if (httpIpv4Resolver == null && httpIpv6Resolver == null) {
            throw new UnknownHostException("resolver server is invalid");
        }

        Record[] records = null;
        if (httpIpv4Resolver != null) {
            try {
                records = httpIpv4Resolver.resolve(new Domain(hostname), null);
            } catch (IOException ignore) {
            }
        }

        if ((records == null || records.length == 0) && httpIpv6Resolver != null) {
            try {
                records = httpIpv6Resolver.resolve(new Domain(hostname), null);
            } catch (IOException ignore) {
            }
        }

        if (records == null || records.length == 0) {
            return null;
        }

        ArrayList<IDnsNetworkAddress> addressList = new ArrayList<>();
        for (Record record : records) {
            if (record.isA() || record.isAAAA()) {
                String source = DnsSource.Doh + ":<" + record.server + ">";
                DnsNetworkAddress address = new DnsNetworkAddress(hostname, record.value, record.timeStamp, source, record.timeStamp);
                addressList.add(address);
            }
        }

        return addressList;
    }

}
