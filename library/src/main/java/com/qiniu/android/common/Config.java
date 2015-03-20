package com.qiniu.android.common;


public final class Config {
    public static final String VERSION = "7.0.4";

    /**
     * 默认上传服务器
     */
    public static final String UP_HOST = "upload.qiniu.com";

    /**
     * 备用上传服务器，当默认服务器网络链接失败时使用
     */
    public static final String UP_HOST_BACKUP = "up.qiniu.com";

    /**
     * 备用上传服务器，当默认服务器网络链接失败时使用
     */
    public static final String UP_IP_BACKUP = "183.136.139.16";

    /**
     * 默认上传服务器
     */
    public static String defaultUpHost = UP_HOST;


    /**
     * 断点上传时的分片大小(可根据网络情况适当调整)
     */
    public static final int CHUNK_SIZE = 256 * 1024;

    /**
     * 断点上传时的分块大小(默认的分块大小, 不建议改变)
     */
    public static final int BLOCK_SIZE = 4 * 1024 * 1024;

    /**
     * 如果文件大小大于此值则使用断点上传, 否则使用Form上传
     */
    public static final int PUT_THRESHOLD = 512 * 1024;

    /**
     * 连接超时时间(默认10s)
     */
    public static final int CONNECT_TIMEOUT = 10 * 1000;

    /**
     * 回复超时时间(默认30s)
     */
    public static final int RESPONSE_TIMEOUT = 30 * 1000;

    /**
     * 上传失败重试次数
     */
    public static final int RETRY_MAX = 5;

    public static final String UTF_8 = "utf-8";

}
