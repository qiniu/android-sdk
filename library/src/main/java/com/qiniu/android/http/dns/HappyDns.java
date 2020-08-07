package com.qiniu.android.http.dns;

import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.dns.Domain;
import com.qiniu.android.dns.IResolver;
import com.qiniu.android.dns.NetworkInfo;
import com.qiniu.android.dns.Record;
import com.qiniu.android.dns.http.DnspodFree;
import com.qiniu.android.utils.Utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yangsen on 2020/6/8
 */
public class HappyDns implements Dns {

    private DnsManager dnsManager;


    public HappyDns(){
        IResolver[] resolvers = new IResolver[2];
        resolvers[0] = new SystemResolver();
        resolvers[1] = new DnspodFree();

        dnsManager = new DnsManager(NetworkInfo.normal, resolvers);
    }

    void setQueryErrorHandler(DnsQueryErrorHandler handler){
        dnsManager.queryErrorHandler = handler;
    }

    @Override
    public List<IDnsNetworkAddress> lookup(String hostname) throws UnknownHostException {
        Domain domain = new Domain(hostname);
        List<IDnsNetworkAddress> addressList = null;
        try {
            Record[] records = dnsManager.queryRecords(domain);
            if (records != null && records.length > 0){
                addressList = new ArrayList<>();
                for (Record record : records) {
                    String source = "";
                    if (record.source == Record.Source.System) {
                        source = "system";
                    } else if (record.source == Record.Source.DnspodFree ||
                            record.source == Record.Source.DnspodEnterprise) {
                        source = "httpdns";
                    } else if (record.source == Record.Source.Unknown) {
                        source = "none";
                    } else {
                        source = "customized";
                    }
                    DnsNetworkAddress address = new DnsNetworkAddress(hostname, record.value, (long)record.ttl, source, record.timeStamp);
                    addressList.add(address);
                }
            }
        } catch (IOException ignored) {
        }
        return addressList;
    }


    private static class SystemResolver implements IResolver{

        @Override
        public Record[] resolve(Domain domain, NetworkInfo info) throws IOException {

            long timestamp = Utils.currentTimestamp();
            int ttl = 120;
            ArrayList<Record> records = new ArrayList<>();
            List<InetAddress> inetAddresses = new SystemDns().lookupInetAddress(domain.domain);
            for(InetAddress inetAddress : inetAddresses){
                Record record = new Record(inetAddress.getHostAddress(), Record.TYPE_A, ttl, timestamp, Record.Source.System);
                records.add(record);
            }
            return records.toArray(new Record[0]);
        }
    }


    interface DnsQueryErrorHandler extends DnsManager.QueryErrorHandler {
    }
}
