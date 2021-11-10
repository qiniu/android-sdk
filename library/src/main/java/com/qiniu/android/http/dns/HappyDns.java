package com.qiniu.android.http.dns;

import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.storage.GlobalConfiguration;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Created by yangsen on 2020/6/8
 */
@Deprecated
public class HappyDns implements Dns {

    private Dns customDns;
    private SystemDns systemDns;

    private DnsQueryErrorHandler errorHandler;

    public HappyDns(){
        int dnsTimeout = GlobalConfiguration.getInstance().dnsResolveTimeout;
        systemDns = new SystemDns(dnsTimeout);
        customDns = GlobalConfiguration.getInstance().dns;
    }

    void setQueryErrorHandler(DnsQueryErrorHandler handler){
        errorHandler = handler;
    }

    @Override
    public List<IDnsNetworkAddress> lookup(String hostname) throws UnknownHostException {
        List<IDnsNetworkAddress> addressList = null;
        int dnsTimeout = GlobalConfiguration.getInstance().dnsResolveTimeout;
        // 自定义 dns
        if (customDns != null) {
            try {
                addressList = customDns.lookup(hostname);
            } catch (IOException e) {
                handleDnsError(e, hostname);
            }
            if (addressList != null && addressList.size() > 0) {
                return addressList;
            }
        }

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
            HttpDns httpDns = new HttpDns(dnsTimeout);
            addressList = httpDns.lookup(hostname);
        } catch (IOException e) {
            handleDnsError(e, hostname);
        }
        if (addressList != null && addressList.size() > 0) {
            return addressList;
        }

        // udp dns
        try {
            UdpDns udpDns = new UdpDns(dnsTimeout);
            addressList = udpDns.lookup(hostname);
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
