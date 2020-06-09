package com.qiniu.android.http.dns;

import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.dns.Domain;
import com.qiniu.android.dns.IResolver;
import com.qiniu.android.dns.NetworkInfo;
import com.qiniu.android.dns.Record;
import com.qiniu.android.dns.http.DnspodFree;
import com.qiniu.android.dns.local.AndroidDnsServer;
import com.qiniu.android.storage.Recorder;
import com.qiniu.android.utils.Utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by yangsen on 2020/6/8
 */
public class HappyDns implements Dns {

    private DnsManager dnsManager;

    public HappyDns(){
        IResolver[] resolvers = new IResolver[3];
        resolvers[0] = new SystemResolver();
        resolvers[1] = AndroidDnsServer.defaultResolver();
        resolvers[2] = new DnspodFree();

        dnsManager = new DnsManager(NetworkInfo.normal, resolvers);
    }

    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        Domain domain = new Domain(hostname);
        List<InetAddress> inetAddressList = null;
        try {
            inetAddressList = Arrays.asList(dnsManager.queryInetAdress(domain));
        } catch (IOException ignored) {
        }
        return inetAddressList;
    }


    private static class SystemResolver implements IResolver{

        @Override
        public Record[] resolve(Domain domain, NetworkInfo info) throws IOException {

            long timestamp = Utils.currentTimestamp();
            int ttl = 120;
            ArrayList<Record> records = new ArrayList<>();
            List<InetAddress> inetAddresses = new SystemDns().lookup(domain.domain);
            for(InetAddress inetAddress : inetAddresses){
                Record record = new Record(inetAddress.getHostAddress(), Record.TYPE_A, ttl, timestamp);
                records.add(record);
            }
            return records.toArray(new Record[0]);
        }
    }
}
