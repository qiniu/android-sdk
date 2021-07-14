package com.qiniu.android.http.dns;

import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.dns.Domain;
import com.qiniu.android.dns.IResolver;
import com.qiniu.android.dns.NetworkInfo;
import com.qiniu.android.dns.Record;
import com.qiniu.android.dns.http.DnspodFree;
import com.qiniu.android.storage.GlobalConfiguration;
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

    private SystemDns systemDns;
    private ServersDns serversDns;
    private HttpDns httpDns;

    private DnsQueryErrorHandler errorHandler;

    public HappyDns(){
        int dnsTimeout = GlobalConfiguration.getInstance().dnsServerResolveTimeout;
        systemDns = new SystemDns(dnsTimeout);
        serversDns = new ServersDns(GlobalConfiguration.getInstance().dnsServers, dnsTimeout);
        httpDns = new HttpDns(dnsTimeout);
    }

    void setQueryErrorHandler(DnsQueryErrorHandler handler){
        errorHandler = handler;
    }

    @Override
    public List<IDnsNetworkAddress> lookup(String hostname) throws UnknownHostException {
        List<IDnsNetworkAddress> addressList = null;

        // 系统 dns
        try {
            addressList = systemDns.lookup(hostname);
        } catch (IOException e) {
            handleDnsError(e, hostname);
        }
        if (addressList != null && addressList.size() > 0) {
            return addressList;
        }

        // 指定 server ip dns
        try {
            addressList = serversDns.lookup(hostname);
        } catch (IOException e) {
            handleDnsError(e, hostname);
        }
        if (addressList != null && addressList.size() > 0) {
            return addressList;
        }

        // http dns
        try {
            addressList = httpDns.lookup(hostname);
        } catch (IOException e) {
            handleDnsError(e, hostname);
        }

        return addressList;
    }

    private void handleDnsError(IOException e, String host) {
        if (errorHandler != null) {
            errorHandler.queryError(e, host);
        }
    }

    interface DnsQueryErrorHandler extends DnsManager.QueryErrorHandler {
    }
}
