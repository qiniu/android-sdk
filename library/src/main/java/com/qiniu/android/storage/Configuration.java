package com.qiniu.android.storage;


import com.qiniu.android.http.Proxy;

import java.io.File;

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
     * 连接超时时间
     */
    public final int connectTimeout;

    /**
     * 服务器响应超时时间
     */
    public final int responseTimeout;

    /**
     * 上传失败重试次数
     */
    public final int retryMax;

    private Configuration(Builder builder){
        upHost = builder.upHost;
        upHostBackup = builder.upHostBackup;
        upIp = builder.upIp;
        upPort = builder.upPort;

        chunkSize = builder.chunkSize;
        putThreshold = builder.putThreshold;

        connectTimeout = builder.connectTimeout;
        responseTimeout = builder.responseTimeout;

        recorder = builder.recorder;
        keyGen = getKeyGen(builder.keyGen);

        retryMax = builder.retryMax;

        proxy = builder.proxy;
    }

    private KeyGenerator getKeyGen(KeyGenerator keyGen) {
        if(keyGen == null) {
            keyGen = new KeyGenerator() {
                @Override
                public String gen(String key, File file) {
                    return key + "_._" + new StringBuffer(file.getAbsolutePath()).reverse();
                }
            };
        }
        return keyGen;
    }

    public static class Builder{
        private String upHost = "upload.qiniu.com";
        private String upHostBackup = "up.qiniu.com";
        private String upIp = "183.136.139.16";
        private int upPort = 80;
        private Recorder recorder = null;
        private KeyGenerator keyGen = null;
        private Proxy proxy = null;

        private int chunkSize = 256 * 1024;
        private int putThreshold = 512 * 1024;
        private int connectTimeout = 10;
        private int responseTimeout = 30;
        private int retryMax = 5;

        public Builder upHost(String upHost){
            this.upHost = upHost;
            return this;
        }

        public Builder recorder(Recorder recorder){
            this.recorder = recorder;
            return this;
        }

        public Builder recorder(Recorder recorder, KeyGenerator keyGen){
            this.recorder = recorder;
            this.keyGen = keyGen;
            return this;
        }

        public Builder upHostBackup(String backup){
            upHostBackup = backup;
            return this;
        }

        public Builder upIp(String ip){
            upIp = ip;
            return this;
        }

        public Builder upPort(int port){
            upPort = port;
            return this;
        }

        public Builder proxy(Proxy proxy){
            this.proxy = proxy;
            return this;
        }

        public Builder chunkSize(int size){
            this.chunkSize = size;
            return this;
        }

        public Builder putThreshhold(int size){
            this.putThreshold = size;
            return this;
        }

        public Builder connectTimeout(int timeout){
            this.connectTimeout = timeout;
            return this;
        }

        public Builder responseTimeout(int timeout){
            this.responseTimeout = timeout;
            return this;
        }

        public Builder retryMax(int times){
            this.retryMax = times;
            return this;
        }

        public Configuration build(){
            return new Configuration(this);
        }
    }

}
