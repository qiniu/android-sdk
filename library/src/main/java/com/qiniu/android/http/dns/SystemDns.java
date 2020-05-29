package com.qiniu.android.http.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Created by yangsen on 2020/5/28
 */
public class SystemDns implements Dns {
    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        return okhttp3.Dns.SYSTEM.lookup(hostname);
    }
}
