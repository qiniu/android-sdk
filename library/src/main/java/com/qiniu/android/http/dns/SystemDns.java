package com.qiniu.android.http.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by yangsen on 2020/5/28
 */
public class SystemDns implements Dns {

    public List<InetAddress> lookupInetAddress(String hostname) throws UnknownHostException {
        InetAddress[] addressArray = InetAddress.getAllByName(hostname);
        return Arrays.asList(addressArray);
    }

    @Override
    public List<IDnsNetworkAddress> lookup(String hostname) throws UnknownHostException {
        ArrayList<IDnsNetworkAddress> addressList = new ArrayList<>();
        InetAddress[] addressArray = InetAddress.getAllByName(hostname);
        for (InetAddress inetAddress : addressArray) {
            DnsNetworkAddress address = new DnsNetworkAddress(inetAddress.getHostName(), inetAddress.getHostAddress(), 120L, null, (new Date()).getTime());
            addressList.add(address);
        }
        return addressList;
    }
}
