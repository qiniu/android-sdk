package com.qiniu.android.http.dns;

import com.qiniu.android.dns.Domain;
import com.qiniu.android.dns.IResolver;
import com.qiniu.android.dns.Record;
import com.qiniu.android.dns.http.DnspodFree;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HttpDns implements Dns {

    private IResolver httpResolver;

    public HttpDns(int timeout) {
        httpResolver = new DnspodFree("119.29.29.29", timeout);
    }

    @Override
    public List<IDnsNetworkAddress> lookup(String hostname) throws UnknownHostException {

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
            DnsNetworkAddress address = new DnsNetworkAddress(hostname, record.value, record.timeStamp, "httpdns", (new Date()).getTime());
            addressList.add(address);
        }

        return addressList;
    }

}
