package com.qiniu.android.storage;

import com.qiniu.android.common.AutoZone;
import com.qiniu.android.common.Zone;
import com.qiniu.android.http.ProxyConfiguration;
import com.qiniu.android.http.UrlConverter;
import com.qiniu.android.http.request.IRequestClient;

import java.io.File;

/**
 * 上传配置信息
 */
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
     * 是否允许使用加速域名，默认为 false
     */
    public final boolean accelerateUploading;

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
        accelerateUploading = builder.accelerateUploading;

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

    /**
     * Configuration Builder
     */
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
        private int writeTimeout = 30;
        private int responseTimeout = 10;
        private int retryMax = 1;
        private int retryInterval = 500;
        private boolean accelerateUploading = false;
        private boolean allowBackupHost = true;
        private UrlConverter urlConverter = null;
        private boolean useConcurrentResumeUpload = false;
        private int resumeUploadVersion = RESUME_UPLOAD_VERSION_V1;
        private int concurrentTaskCount = 3;

        /**
         * 构造函数
         */
        public Builder() {
        }

        /**
         * Builder 构造方法
         *
         * @param requestClient 请求的客户端
         * @return Builder
         */
        public Builder requestClient(IRequestClient requestClient) {
            this.requestClient = requestClient;
            return this;
        }

        /**
         * 配置请求的 Zone
         *
         * @param zone 请求的 Zone
         * @return Builder
         */
        public Builder zone(Zone zone) {
            this.zone = zone;
            return this;
        }

        /**
         * 配置上传的 Recorder，Recorder 可以记录上传的进度，使上传支持断点续传
         *
         * @param recorder 请求的 Recorder
         * @return Builder
         */
        public Builder recorder(Recorder recorder) {
            this.recorder = recorder;
            return this;
        }

        /**
         * 配置上传的 Recorder，Recorder 可以记录上传的进度，使上传支持断点续传
         *
         * @param recorder 请求的 Recorder
         * @param keyGen   上传记录 key 的生成器
         * @return Builder
         */
        public Builder recorder(Recorder recorder, KeyGenerator keyGen) {
            this.recorder = recorder;
            this.keyGen = keyGen;
            return this;
        }

        /**
         * 配置请求的 ProxyConfiguration
         *
         * @param proxy 请求的代理配置
         * @return Builder
         */
        public Builder proxy(ProxyConfiguration proxy) {
            this.proxy = proxy;
            return this;
        }

        /**
         * 配置分片上传时的分片大小
         *
         * @param size 分片大小，单位：B
         * @return Builder
         */
        public Builder chunkSize(int size) {
            this.chunkSize = size;
            return this;
        }

        /**
         * 配置分片上传的阈值，大于此值会使用分片上传，小于等于此值会使用表单上传
         *
         * @param size 阈值，单位：B
         * @return Builder
         */
        public Builder putThreshold(int size) {
            this.putThreshold = size;
            return this;
        }

        /**
         * 配置请求建立连接的超时时间
         *
         * @param timeout 超时时间，单位：秒
         * @return Builder
         */
        public Builder connectTimeout(int timeout) {
            this.connectTimeout = timeout;
            return this;
        }

        /**
         * 配置请求发送数据的超时时间
         *
         * @param timeout 超时时间，单位：秒
         * @return Builder
         */
        public Builder writeTimeout(int timeout) {
            this.writeTimeout = timeout;
            return this;
        }

        /**
         * 配置请求接收数据的超时时间
         *
         * @param timeout 超时时间，单位：秒
         * @return Builder
         */
        public Builder responseTimeout(int timeout) {
            this.responseTimeout = timeout;
            return this;
        }

        /**
         * 配置请求单个域名最大的重试次数，一个上传请求可能会有多个主备域名
         *
         * @param times 请求单个域名最大的重试次数
         * @return Builder
         */
        public Builder retryMax(int times) {
            this.retryMax = times;
            return this;
        }

        /**
         * 配置请求重试时间间隔
         *
         * @param retryInterval 请求重试时间间隔，单位：秒
         * @return Builder
         */
        public Builder retryInterval(int retryInterval) {
            this.retryInterval = retryInterval;
            return this;
        }

        /**
         * 配置是否允许使用加速域名
         * 注：加速域名会额外收费
         *
         * @param isAllow 是否允许使用加速域名
         * @return Builder
         */
        public Builder accelerateUploading(boolean isAllow) {
            this.accelerateUploading = isAllow;
            return this;
        }

        /**
         * 配置是否允许使用备用域名，如果不允许则仅会使用一个域名进行上传
         * 注：如果配置为 false 可能会影响上传的成功率
         *
         * @param isAllow 是否允许使用备用域名
         * @return Builder
         */
        public Builder allowBackupHost(boolean isAllow) {
            this.allowBackupHost = isAllow;
            return this;
        }

        /**
         * 配置请求 Url 的拦截器
         *
         * @param converter 请求 Url 的拦截器
         * @return Builder
         */
        public Builder urlConverter(UrlConverter converter) {
            this.urlConverter = converter;
            return this;
        }

        /**
         * 配置上传是否允许使用并发分片方式
         *
         * @param useConcurrentResumeUpload 上传是否允许使用并发分片方式
         * @return Builder
         */
        public Builder useConcurrentResumeUpload(boolean useConcurrentResumeUpload) {
            this.useConcurrentResumeUpload = useConcurrentResumeUpload;
            return this;
        }

        /**
         * 配置分片上传的版本
         *
         * @param resumeUploadVersion 分片上传的版本
         * @return Builder
         */
        public Builder resumeUploadVersion(int resumeUploadVersion) {
            this.resumeUploadVersion = resumeUploadVersion;
            return this;
        }

        /**
         * 配置分片上传的并发度
         *
         * @param concurrentTaskCount 分片上传的并发度
         * @return Builder
         */
        public Builder concurrentTaskCount(int concurrentTaskCount) {
            this.concurrentTaskCount = concurrentTaskCount;
            return this;
        }

        /**
         * 配置是否使用 HTTP 请求
         *
         * @param useHttps 是否使用 HTTP 请求
         * @return Builder
         */
        public Builder useHttps(boolean useHttps) {
            this.useHttps = useHttps;
            return this;
        }

        /**
         * 生成 Configuration
         *
         * @return Configuration
         */
        public Configuration build() {
            return new Configuration(this);
        }
    }

}
