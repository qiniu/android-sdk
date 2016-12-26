package com.qiniu.android.common;

import com.qiniu.android.dns.DnsManager;

/**
 * Created by bailong on 15/10/10.
 */
public abstract class Zone {

    /**
     * 华东机房, http
     */
    public static final Zone zone0 =
            createZone("upload.qiniu.com", "up.qiniu.com", "183.136.139.10", "115.231.182.136");

    private static Zone createZone(String upHost, String upHostBackup, String upIp, String upIp2) {
        String[] upIps = {upIp, upIp2};
        ServiceAddress up = new ServiceAddress("http://" + upHost, upIps);
        ServiceAddress upBackup = new ServiceAddress("http://" + upHostBackup, upIps);
        return new FixedZone(up, upBackup);
    }


    /**
     * 默认上传服务器
     */
    public abstract ServiceAddress upHost(String token);

    /**
     * 备用上传服务器，当默认服务器网络连接失败时使用
     */
    public abstract ServiceAddress upHostBackup(String token);

    public void addDnsIp(DnsManager dns) {
        upHost("").addIpToDns(dns);
        upHostBackup("").addIpToDns(dns);
    }
}
