package com.qiniu.android.bigdata;

import com.qiniu.android.http.ProxyConfiguration;

/**
 * Created by long on 2017/7/25.
 */

public final class Configuration implements Cloneable {
    public String pipelineHost = "https://pipeline.qiniu.com";

    public ProxyConfiguration proxy;


    /**
     * 连接超时时间，单位 秒
     */
    public int connectTimeout = 3;

    /**
     * 服务器响应超时时间 单位 秒
     */
    public int responseTimeout = 10;

    /**
     * 上传失败重试次数
     */
    public int retryMax = 3;


}
