package com.qiniu.android.storage;

import com.qiniu.android.common.AutoZone;
import com.qiniu.android.common.Zone;
import com.qiniu.android.http.Dns;
import com.qiniu.android.http.ProxyConfiguration;
import com.qiniu.android.http.UrlConverter;

import java.io.File;

public final class Configuration {

    /**
     * 断点上传时的分块大小(默认的分块大小, 不建议改变)
     */
    public static final int BLOCK_SIZE = 4 * 1024 * 1024;

    public final Recorder recorder;
    public final KeyGenerator keyGen;

    public final ProxyConfiguration proxy;

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

    /**
     * dns 解析客户端
     */
    public Dns dns;

    /**
     * 上传区域
     */
    public Zone zone;

    /**
     * 使用https域名
     */
    public boolean useHttps;

    /**
     * dns预取缓存时间
     */
    public long dnsCacheTimeMs;


    private Configuration(Builder builder) {
        useHttps = builder.useHttps;

        chunkSize = builder.chunkSize;
        putThreshold = builder.putThreshold;

        connectTimeout = builder.connectTimeout;
        responseTimeout = builder.responseTimeout;

        recorder = builder.recorder;
        keyGen = getKeyGen(builder.keyGen);

        retryMax = builder.retryMax;

        proxy = builder.proxy;

        dnsCacheTimeMs = builder.dnsCacheTimeMs;

        urlConverter = builder.urlConverter;
        AutoZone autoZone = null;

        autoZone = new AutoZone(builder.useHttps);

        zone = builder.zone == null ? autoZone : builder.zone;
        dns = builder.dns;
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
        private Zone zone = null;
        private Recorder recorder = null;
        private KeyGenerator keyGen = null;
        private ProxyConfiguration proxy = null;

        private boolean useHttps = true;
        private int chunkSize = 2 * 1024 * 1024;
        private int putThreshold = 4 * 1024 * 1024;
        private int connectTimeout = 10;
        private int responseTimeout = 60;
        private int retryMax = 3;
        private UrlConverter urlConverter = null;
        private Dns dns = null;
        private long dnsCacheTimeMs = 60 * 60 * 24 * 1000;

        public Builder zone(Zone zone) {
            this.zone = zone;
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

        public Builder proxy(ProxyConfiguration proxy) {
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

        public Builder dns(Dns dns) {
            this.dns = dns;
            return this;
        }

        public Builder useHttps(boolean useHttps) {
            this.useHttps = useHttps;
            return this;
        }

        public Builder dnsCacheTimeMs(long dnsCacheTimeMs) {
            this.dnsCacheTimeMs = dnsCacheTimeMs;
            return this;
        }

        public Configuration build() {
            return new Configuration(this);
        }
    }

}
