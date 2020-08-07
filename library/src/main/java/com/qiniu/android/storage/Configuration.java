package com.qiniu.android.storage;

import com.qiniu.android.common.AutoZone;
import com.qiniu.android.common.Zone;
import com.qiniu.android.http.dns.Dns;
import com.qiniu.android.http.ProxyConfiguration;
import com.qiniu.android.http.UrlConverter;

import java.io.File;

public final class Configuration {

    /**
     * 上传区域
     */
    public final Zone zone;

    /**
     * 断点上传时的分块大小(默认的分块大小, 不建议改变) 【已无效】
     */
    public static final int BLOCK_SIZE = 4 * 1024 * 1024;

    /**
     * 断点上传时的分片大小(可根据网络情况适当调整)
     */
    public final int chunkSize;

    /**
     * 如果文件大小大于此值则使用断点上传, 否则使用Form上传
     */
    public final int putThreshold;

    /**
     * 上传失败重试次数 默认1次
     */
    public final int retryMax;

    /**
     * 重试时间间隔 单位：毫秒 默认500
     */
    public final int retryInterval;

    /**
     * 连接超时时间，单位 秒
     */
    public final int connectTimeout;

    /**
     * 服务器响应超时时间 单位 秒
     */
    public final int responseTimeout;

    /**
     * 使用https域名
     */
    public final boolean useHttps;

    /**
     * 是否使用并发上传 默认为false
     */
    public final boolean useConcurrentResumeUpload;

    /**
     * 并发分片上传的并发任务个数，在concurrentResumeUpload为YES时有效，默认为3个
     */
    public final int concurrentTaskCount;

    /**
     * 重试时是否允许使用备用上传域名，默认为true
     */
    public final boolean allowBackupHost;

    /**
     *  持久化记录接口，可以实现将记录持久化到文件，数据库等
     */
    public final Recorder recorder;

    /**
     *  为持久化上传记录，根据上传的key以及文件名 生成持久化的记录key
     */
    public final KeyGenerator keyGen;

    /**
     *  上传请求代理配置信息
     */
    public final ProxyConfiguration proxy;

    /**
     * 特别定制的url转换
     */
    public final UrlConverter urlConverter;


    private Configuration(Builder builder) {
        chunkSize = builder.chunkSize;
        putThreshold = builder.putThreshold;

        connectTimeout = builder.connectTimeout;
        responseTimeout = builder.responseTimeout;

        recorder = builder.recorder;
        keyGen = getKeyGen(builder.keyGen);

        retryMax = builder.retryMax;
        retryInterval = builder.retryInterval;

        allowBackupHost = builder.allowBackupHost;

        proxy = builder.proxy;

        urlConverter = builder.urlConverter;

        useHttps = builder.useHttps;

        useConcurrentResumeUpload = builder.useConcurrentResumeUpload;
        concurrentTaskCount = builder.concurrentTaskCount;

        zone = builder.zone != null ? builder.zone : new AutoZone();
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
        private int connectTimeout = 60;
        private int responseTimeout = 60;
        private int retryMax = 1;
        private int retryInterval = 500;
        private boolean allowBackupHost = true;
        private UrlConverter urlConverter = null;
        private boolean useConcurrentResumeUpload = false;
        private int concurrentTaskCount = 3;

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

        public Builder putThreshold(int size) {
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

        public Builder retryInterval(int retryInterval) {
            this.retryInterval = retryInterval;
            return this;
        }

        public Builder allowBackupHost(boolean isAllow) {
            this.allowBackupHost = isAllow;
            return this;
        }

        public Builder urlConverter(UrlConverter converter) {
            this.urlConverter = converter;
            return this;
        }

        public Builder useConcurrentResumeUpload(boolean useConcurrentResumeUpload) {
            this.useConcurrentResumeUpload = useConcurrentResumeUpload;
            return this;
        }

        public Builder concurrentTaskCount(int concurrentTaskCount) {
            this.concurrentTaskCount = concurrentTaskCount;
            return this;
        }

        public Builder useHttps(boolean useHttps) {
            this.useHttps = useHttps;
            return this;
        }

        public Configuration build() {
            return new Configuration(this);
        }
    }

}
