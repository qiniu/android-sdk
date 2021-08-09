package com.qiniu.android.http.dns;

import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.storage.GlobalConfiguration;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Created by yangsen on 2020/6/8
 */
public class HappyDns implements Dns {

    private SystemDns systemDns;
    private HttpDns httpDns;

    private DnsQueryErrorHandler errorHandler;

    public HappyDns(){
        int dnsTimeout = GlobalConfiguration.getInstance().dnsResolveTimeout;
        systemDns = new SystemDns(dnsTimeout);
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
