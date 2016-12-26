package com.qiniu.android.storage;


import com.qiniu.android.common.Zone;
import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.dns.IResolver;
import com.qiniu.android.dns.NetworkInfo;
import com.qiniu.android.dns.local.AndroidDnsServer;
import com.qiniu.android.dns.local.Resolver;

import java.io.IOException;
import java.net.InetAddress;

public final class Configuration {
    
    public static final int BLOCK_SIZE = 4 * 1024 * 1024;

    /**
     * 连接超时时间，单位 秒
     */
    public final int connectTimeout;

    /**
     * 服务器响应超时时间 单位 秒
     */
    public final int responseTimeout;

    /**
     * 上传失败重试次数
     */
    public final int retryMax;


    /**
     * dns 解析客户端
     */
    public DnsManager dns;

    /**
     * 上传区域
     */
    public Zone zone;

    private Configuration(Builder builder) {
        connectTimeout = builder.connectTimeout;
        responseTimeout = builder.responseTimeout;

        retryMax = builder.retryMax;

        zone = builder.zone == null ? Zone.zone0 : builder.zone;
        dns = initDns(builder);
    }

    private DnsManager initDns(Builder builder) {
        DnsManager d = builder.dns;
        if (d != null) {
            zone.addDnsIp(d);
        }

        return d;
    }


    public static class Builder {
        private Zone zone = null;
        private int connectTimeout = 10;
        private int responseTimeout = 60;
        private int retryMax = 3;
        private DnsManager dns = null;

        public Builder() {
            IResolver r1 = AndroidDnsServer.defaultResolver();
            IResolver r2 = null;
            try {
                r2 = new Resolver(InetAddress.getByName("119.29.29.29"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            dns = new DnsManager(NetworkInfo.normal, new IResolver[]{r1, r2});
        }

        public Builder zone(Zone zone) {
            this.zone = zone;
            return this;
        }

        public Builder connectTimeout(int timeout) {
            this.connectTimeout = timeout;
            return this;
        }

        public Builder responseTimeout(int timeout) {
            this.responseTimeout = timeout;
            return this;
        }

        public Builder retryMax(int times) {
            this.retryMax = times;
            return this;
        }

        public Builder dns(DnsManager dns) {
            this.dns = dns;
            return this;
        }

        public Configuration build() {
            return new Configuration(this);
        }
    }

}
