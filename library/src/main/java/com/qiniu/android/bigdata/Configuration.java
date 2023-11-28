package com.qiniu.android.bigdata;

import com.qiniu.android.http.ProxyConfiguration;

/**
 * Created by long on 2017/7/25.
 */

public final class Configuration implements Cloneable {

    /**
     * pipelineHost
     */
    public String pipelineHost = "https://pipeline.qiniu.com";

    /**
     * 请求 proxy
     */
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
     * 构造函数
     */
    public Configuration() {
    }

    /**
     * Configuration copy
     *
     * @param config 待 copy 对象
     * @return Configuration
     */
    public static Configuration copy(Configuration config) {
        if (config == null) {
            return new Configuration();
        }
        try {
            return config.clone();
        } catch (CloneNotSupportedException e) {
            return new Configuration();
        }
    }

    /**
     * Configuration clone
     *
     * @return Configuration
     */
    public Configuration clone() throws CloneNotSupportedException {
        return (Configuration) super.clone();
    }
}
