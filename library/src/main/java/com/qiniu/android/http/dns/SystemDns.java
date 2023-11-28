package com.qiniu.android.http.dns;

import com.qiniu.android.storage.GlobalConfiguration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by yangsen on 2020/5/28
 */
public class SystemDns extends BaseDns implements Dns {

    /**
     * 构造函数
     */
    public SystemDns() {
    }

    /**
     * 构造函数
     *
     * @param timeout DNS 解析超时时间
     */
    public SystemDns(int timeout) {
        this.timeout = timeout;
    }

    /**
     * DNS 解析域名
     *
     * @param hostname 域名
     * @return 解析结果
     * @throws UnknownHostException 异常
     */
    public List<InetAddress> lookupInetAddress(final String hostname) throws UnknownHostException {
        if (hostname == null) {
            throw new UnknownHostException("hostname is null");
        } else {
            try {
                Future<List<InetAddress>> task = executor.submit(new Callable<List<InetAddress>>() {
                    @Override
                    public List<InetAddress> call() throws Exception {
                        return Arrays.asList(InetAddress.getAllByName(hostname));
                    }
                });
                return task.get(timeout, TimeUnit.SECONDS);
            } catch (Exception var4) {
                UnknownHostException unknownHostException =
                        new UnknownHostException("dns broken when lookup of " + hostname);
                unknownHostException.initCause(var4);
                throw unknownHostException;
            }
        }
    }

    /**
     * DNS 解析域名
     *
     * @param hostname 域名
     * @return 解析结果
     * @throws UnknownHostException 异常
     */
    @Override
    public List<IDnsNetworkAddress> lookup(String hostname) throws UnknownHostException {
        long timestamp = new Date().getTime() / 1000;
        long defaultTTL = GlobalConfiguration.getInstance().dnsCacheTime;
        ArrayList<IDnsNetworkAddress> addressList = new ArrayList<>();
        List<InetAddress> inetAddressList = lookupInetAddress(hostname);
        for (InetAddress inetAddress : inetAddressList) {
            DnsNetworkAddress address = new DnsNetworkAddress(inetAddress.getHostName(), inetAddress.getHostAddress(), defaultTTL, DnsSource.System, timestamp);
            addressList.add(address);
        }
        return addressList;
    }
}
