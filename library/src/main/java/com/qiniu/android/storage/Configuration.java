package com.qiniu.android.storage;


import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.dns.IResolver;
import com.qiniu.android.dns.NetworkInfo;
import com.qiniu.android.dns.local.AndroidDnsServer;
import com.qiniu.android.dns.local.Resolver;
import com.qiniu.android.http.Proxy;
import com.qiniu.android.http.UrlConverter;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

public final class Configuration {

    /**
     * 断点上传时的分块大小(默认的分块大小, 不建议改变)
     */
    public static final int BLOCK_SIZE = 4 * 1024 * 1024;

    /**
     * 默认上传服务器
     */
    public final String upHost;

    /**
     * 备用上传服务器，当默认服务器网络连接失败时使用
     */
    public final String upHostBackup;

    /**
     * 备用上传服务器Ip，当默认服务器域名解析失败时使用
     */
    public final String upIp;
    public final String upIp2;

    /**
     * 指定上传端口
     */
    public final int upPort;

    public final Recorder recorder;
    public final KeyGenerator keyGen;

    public final Proxy proxy;

    /**
     * 断点上传时的分片大小(可根据网络情况适当调整)
     */
    public final int chunkSize;

    /**
     * 如果文件大小大于此值则使用断点上传, 否则使用Form上传
     */
    public final int putThreshold;

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
     * 特别定制的url转换
     */
    public UrlConverter urlConverter;

    public DnsManager dns;

    private Configuration(Builder builder) {
        upHost = builder.upHost;
        upHostBackup = builder.upHostBackup;
        upIp = builder.upIp;
        upIp2 = builder.upIp2;
        upPort = getPort(builder);

        chunkSize = builder.chunkSize;
        putThreshold = builder.putThreshold;

        connectTimeout = builder.connectTimeout;
        responseTimeout = builder.responseTimeout;

        recorder = builder.recorder;
        keyGen = getKeyGen(builder.keyGen);

        retryMax = builder.retryMax;

        proxy = builder.proxy;

        urlConverter = builder.urlConverter;

        dns = initDns(builder);
    }

    private static DnsManager initDns(Builder builder) {
        DnsManager d = builder.dns;
        if (d == null) {
            IResolver r1 = AndroidDnsServer.defaultResolver();
            IResolver r2 = null;
            try {
                r2 = new Resolver(InetAddress.getByName("223.6.6.6"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            d = new DnsManager(NetworkInfo.normal, new IResolver[]{r1, r2});
        }
        d.putHosts("upload.qiniu.com", builder.upIp);
        d.putHosts("upload.qiniu.com", builder.upIp2);
        d.putHosts("up.qiniu.com", builder.upIp);
        d.putHosts("up.qiniu.com", builder.upIp2);
        return d;
    }

    private static int getPort(Builder builder) {
        if (builder.urlConverter != null) {
            return 80;
        }
        return builder.upPort;
    }

    private KeyGenerator getKeyGen(KeyGenerator keyGen) {
        if (keyGen == null) {
            keyGen = new KeyGenerator() {
                @Override
                public String gen(String key, File file) {
                    return key + "_._" + new StringBuffer(file.getAbsolutePath()).reverse();
                }
            };
        }
        return keyGen;
    }

    public static class Builder {
        private String upHost;
        private String upHostBackup;
        private String upIp;
        private String upIp2;
        private int upPort;

        private Recorder recorder = null;
        private KeyGenerator keyGen = null;
        private Proxy proxy = null;

        private int chunkSize = 256 * 1024;
        private int putThreshold = 512 * 1024;
        private int connectTimeout = 10;
        private int responseTimeout = 60;
        private int retryMax = 5;
        private UrlConverter urlConverter = null;
        private DnsManager dns = null;

        public Builder() {
            this.upHost = Zone.zone0.upHost;
            this.upHostBackup = Zone.zone0.upHostBackup;
            this.upIp = Zone.zone0.upIp;
            this.upIp2 = Zone.zone0.upIp2;
            this.upPort = 8888;
        }

        public Builder zone(Zone zone) {
            this.upHost = zone.upHost;
            this.upHostBackup = zone.upHostBackup;
            this.upIp = zone.upIp;
            return this;
        }

        public Builder recorder(Recorder recorder) {
            this.recorder = recorder;
            return this;
        }

        public Builder recorder(Recorder recorder, KeyGenerator keyGen) {
            this.recorder = recorder;
            this.keyGen = keyGen;
            return this;
        }

        public Builder upPort(int port) {
            upPort = port;
            return this;
        }

        public Builder proxy(Proxy proxy) {
            this.proxy = proxy;
            return this;
        }

        public Builder chunkSize(int size) {
            this.chunkSize = size;
            return this;
        }

        public Builder putThreshhold(int size) {
            this.putThreshold = size;
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

        public Builder urlConverter(UrlConverter converter) {
            this.urlConverter = converter;
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
