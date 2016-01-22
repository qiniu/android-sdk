package com.qiniu.android.common;

import com.qiniu.android.dns.DnsManager;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by bailong on 15/10/10.
 */
public final class ServiceAddress {
    public final URI address;
    public final String[] backupIps;

    public ServiceAddress(String address, String[] backupIps) {
        this.address = uri(address);
        this.backupIps = backupIps == null ? new String[0] : backupIps;
    }

    public ServiceAddress(String address) {
        this(address, null);
    }

    private static URI uri(String address) {
        try {
            return new URI(address);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void addIpToDns(DnsManager d) {
        for (String ip : backupIps) {
            d.putHosts(address.getHost(), ip);
        }
    }
}
