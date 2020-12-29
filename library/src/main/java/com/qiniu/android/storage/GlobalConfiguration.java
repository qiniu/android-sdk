package com.qiniu.android.storage;

import com.qiniu.android.http.dns.Dns;
import com.qiniu.android.utils.Utils;

public class GlobalConfiguration {

    /**
     * 是否开启dns预解析 默认开启
     */
    public boolean isDnsOpen = true;

    /**
     *   dns 预取失败后 会进行重新预取  dnsRepreHostNum为最多尝试次数
     */
    public int dnsRepreHostNum = 2;

    /**
     *   dns预取缓存时间  单位：秒
     */
    public int dnsCacheTime = 120;

    /**
     *   自定义DNS解析客户端host
     */
    public Dns dns = null;

    /**
     *   自定义DNS解析客户端host
     */
    public String dnsCacheDir = Utils.sdkDirectory() + "/dnsCache/";

    /**
     *   Host全局冻结时间  单位：秒   默认：30  推荐范围：[10 ~ 60]
     *   当某个Host的上传失败后并且可能短时间无法恢复，会冻结该Host，globalHostFrozenTime为全局冻结时间
     *   Host全局冻结时间  单位：秒   默认：10  推荐范围：[5 ~ 30]
     *   当某个Host的上传失败后并且可能短时间无法恢复，会冻结该Host
     */
    public int globalHostFrozenTime = 10;

    /**
     *   Host局部冻结时间，只会影响当前上传操作  单位：秒   默认：5*60  推荐范围：[60 ~ 10*60]
     *   当某个Host的上传失败后并且短时间可能会恢复，会局部冻结该Host
     */
    public int partialHostFrozenTime = 5*60;


    private static GlobalConfiguration configuration = new GlobalConfiguration();
    private GlobalConfiguration(){
    }
    public static GlobalConfiguration getInstance(){
        return configuration;
    }
}
