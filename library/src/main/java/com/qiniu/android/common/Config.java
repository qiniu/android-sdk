package com.qiniu.android.common;

/**
 * Created by bailong on 14/10/8.
 */
public final class Config {
    public static final String VERSION = "7.0.1";

    public static final String UP_HOST = "upload.qiniu.com";
    public static final String UP_HOST_BACKUP = "up.qiniu.com";

    public static final int CHUNK_SIZE = 256 * 1024;
    public static final int BLOCK_SIZE = 4 * 1024 * 1024;

    public static final int PUT_THRESHOLD = 512 * 1024;

    public static final int CONNECT_TIMEOUT = 10 * 1000;
    public static final int RESPONSE_TIMEOUT = 30 * 1000;
    public static final int RETRY_MAX = 5;

    public static final String CHARSET = "utf-8";

}
