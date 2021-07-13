package com.qiniu.android.http.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * Created by yangsen on 2020/5/28
 */
public class SystemDns implements Dns {

    private int timeout = 10;

    public SystemDns() {
    }

    public SystemDns(int timeout) {
        this.timeout = timeout;
    }

    public List<InetAddress> lookupInetAddress(final String hostname) throws UnknownHostException {
        if (hostname == null) {
            throw new UnknownHostException("hostname is null");
        } else {
            try {
                FutureTask<List<InetAddress>> task = new FutureTask<>(
                        new Callable<List<InetAddress>>() {
                            @Override
                            public List<InetAddress> call() throws Exception {
                                return Arrays.asList(InetAddress.getAllByName(hostname));
                            }
                        });
                new Thread(task).start();
                return task.get(timeout, TimeUnit.SECONDS);
            } catch (Exception var4) {
                UnknownHostException unknownHostException =
                        new UnknownHostException("Broken system behaviour for dns lookup of " + hostname);
                unknownHostException.initCause(var4);
                throw unknownHostException;
            }
        }
    }

    @Override
    public List<IDnsNetworkAddress> lookup(String hostname) throws UnknownHostException {
        ArrayList<IDnsNetworkAddress> addressList = new ArrayList<>();
        List<InetAddress> inetAddressList = lookupInetAddress(hostname);
        for (InetAddress inetAddress : inetAddressList) {
            DnsNetworkAddress address = new DnsNetworkAddress(inetAddress.getHostName(), inetAddress.getHostAddress(), 120L, null, (new Date()).getTime());
            addressList.add(address);
        }
        return addressList;
    }
}
