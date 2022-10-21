package com.qiniu.android.storage;

import com.qiniu.android.common.AutoZone;
import com.qiniu.android.common.Zone;
import com.qiniu.android.http.ProxyConfiguration;
import com.qiniu.android.http.UrlConverter;
import com.qiniu.android.http.request.IRequestClient;

import java.io.File;

public final class Configuration {

    /**
     * 分片上传版本 V1
     */
    public static int RESUME_UPLOAD_VERSION_V1 = 0;
    /**
     * 分片上传版本 V2
     */
    public static int RESUME_UPLOAD_VERSION_V2 = 1;

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
     * 上传失败重试次数 默认 1 次
     */
    public final int retryMax;

    /**
     * 重试时间间隔 单位：毫秒 默认500
     */
    public final int retryInterval;

    /**
     * 连接超时时间 单位 秒，默认：10
     * 注：每个文件上传肯能存在多个操作，当每个操作失败时，可能存在多个请求重试。
     */
    public final int connectTimeout;

    /**
     * write 响应超时时间 单位 秒，默认：30
     * 注：每个文件上传肯能存在多个操作，当每个操作失败时，可能存在多个请求重试。
     */
    public final int writeTimeout;

    /**
     * 服务器响应超时时间，对应到 readTimeout 单位 秒，默认：10
     * 注：每个文件上传肯能存在多个操作，当每个操作失败时，可能存在多个请求重试。
     */
    public final int responseTimeout;

    /**
     * 使用https域名
     */
    public final boolean useHttps;

    /**
     * 单个文件是否开启并发分片上传，默认为false
     * 单个文件大小大于4M时，会采用分片上传，每个分片会已单独的请求进行上传操作，多个上传操作可以使用并发，
     * 也可以采用串行，采用并发时，可以设置并发的个数(对concurrentTaskCount进行设置)。
     */
    public final boolean useConcurrentResumeUpload;

    /**
     * 分片上传版本
     */
    public final int resumeUploadVersion;

    /**
     * 并发分片上传的并发任务个数，在concurrentResumeUpload为true时有效，默认为3个
     */
    public final int concurrentTaskCount;

    /**
     * 重试时是否允许使用备用上传域名，默认为true
     */
    public final boolean allowBackupHost;

    /**
     * 持久化记录接口，可以实现将记录持久化到文件，数据库等
     */
    public final Recorder recorder;

    /**
     * 为持久化上传记录，根据上传的key以及文件名 生成持久化的记录key
     */
    public final KeyGenerator keyGen;

    /**
     * 上传请求代理配置信息
     */
    public final ProxyConfiguration proxy;

    /**
     * 特别定制的url转换
     */
    public final UrlConverter urlConverter;

    /**
     * 指定 client
     */
    public final IRequestClient requestClient;

    private Configuration(Builder builder) {
        requestClient = builder.requestClient;
        useConcurrentResumeUpload = builder.useConcurrentResumeUpload;
        resumeUploadVersion = builder.resumeUploadVersion;
        concurrentTaskCount = builder.concurrentTaskCount;

        if (builder.resumeUploadVersion == RESUME_UPLOAD_VERSION_V1) {
            if (builder.chunkSize < 1024) {
                builder.chunkSize = 1024;
            }
        } else if (builder.resumeUploadVersion == RESUME_UPLOAD_VERSION_V2) {
            if (builder.chunkSize < 1024 * 1024) {
                builder.chunkSize = 1024 * 1024;
            }
        }
        chunkSize = builder.chunkSize;

        putThreshold = builder.putThreshold;

        connectTimeout = builder.connectTimeout;
        writeTimeout = builder.writeTimeout;
        responseTimeout = builder.responseTimeout;

        recorder = builder.recorder;
        keyGen = getKeyGen(builder.keyGen);

        retryMax = builder.retryMax;
        retryInterval = builder.retryInterval;

        allowBackupHost = builder.allowBackupHost;

        proxy = builder.proxy;

        urlConverter = builder.urlConverter;

        useHttps = builder.useHttps;

        zone = builder.zone != null ? builder.zone : new AutoZone();
    }

    private KeyGenerator getKeyGen(KeyGenerator keyGen) {
        if (keyGen == null) {
            keyGen = new KeyGenerator() {
                @Override
                public String gen(String key, File file) {
                    return key + "_._" + new StringBuffer(file.getAbsolutePath()).reverse();
                }

                @Override
                public String gen(String key, String sourceId) {
                    if (sourceId == null) {
                        sourceId = "";
                    }
                    return key + "_._" + sourceId;
                }
            };
        }
        return keyGen;
    }

    public static class Builder {
        private IRequestClient requestClient = null;
        private Zone zone = null;
        private Recorder recorder = null;
        private KeyGenerator keyGen = null;
        private ProxyConfiguration proxy = null;

        private boolean useHttps = true;
        private int chunkSize = 2 * 1024 * 1024;
        private int putThreshold = 4 * 1024 * 1024;
        private int connectTimeout = 10;
        private  int writeTimeout = 30;
        private int responseTimeout = 10;
        private int retryMax = 1;
        private int retryInterval = 500;
        private boolean allowBackupHost = true;
        private UrlConverter urlConverter = null;
        private boolean useConcurrentResumeUpload = false;
        private int resumeUploadVersion = RESUME_UPLOAD_VERSION_V1;
        private int concurrentTaskCount = 3;

        public Builder requestClient(IRequestClient requestClient) {
            this.requestClient = requestClient;
            return this;
        }

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

        public Builder writeTimeout(int timeout) {
            this.writeTimeout = timeout;
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

        public Builder resumeUploadVersion(int resumeUploadVersion) {
            this.resumeUploadVersion = resumeUploadVersion;
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
