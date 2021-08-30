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

public class HttpDns implements Dns {

    private IResolver httpResolver;

    public HttpDns(int timeout) {
        String[] dohServers = GlobalConfiguration.getInstance().dohServers;
        if (dohServers != null && dohServers.length > 0) {
            httpResolver = new DohResolver(dohServers, Record.TYPE_A, timeout);
        }
    }

    @Override
    public List<IDnsNetworkAddress> lookup(String hostname) throws UnknownHostException {
        if (!GlobalConfiguration.getInstance().dohEnable) {
            return null;
        }

        if (httpResolver == null) {
            throw new UnknownHostException("resolver server is invalid");
        }

        Record[] records = null;
        try {
            records = httpResolver.resolve(new Domain(hostname), null);
        } catch (IOException ignore) {
            throw new UnknownHostException(ignore.toString());
        }

        if (records == null || records.length == 0) {
            return null;
        }

        ArrayList<IDnsNetworkAddress> addressList = new ArrayList<>();
        for (Record record : records) {
            String source = DnsSource.Doh + ":<" + record.server + ">";
            DnsNetworkAddress address = new DnsNetworkAddress(hostname, record.value, record.timeStamp, source, record.timeStamp);
            addressList.add(address);
        }

        return addressList;
    }

}
