package com.qiniu.android.http.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by yangsen on 2020/5/28
 */
public class SystemDns implements Dns {

    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        InetAddress[] inetAddresses = InetAddress.getAllByName(hostname);
        return Arrays.asList(inetAddresses);
    }
}
