package com.qiniu.android.storage;

import com.qiniu.android.http.dns.Dns;
import com.qiniu.android.utils.Utils;

public class GlobalConfiguration {

    /**
     * 是否开启dns预解析 默认开启
     */
    public boolean isDnsOpen = true;

    /**
     *   dns 预取失败后 会进行重新预取  rePreHostNum为最多尝试次数
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
     *  是否开启网络检测
     */
    public boolean isCheckOpen = true;

    /**
     *  单个IP一次检测次数 默认：2次
     */
    public int maxCheckCount = 2;

    /**
     * 单个IP检测的最长时间 maxTime >= 1 && maxTime <= 600  默认：9秒
     */
    public int maxCheckTime = 9;

    /**
     *   Host全局冻结时间  单位：秒   默认：30  推荐范围：[10 ~ 60]
     *   当某个Host的上传失败后并且可能短时间无法恢复，会冻结该Host，globalHostFrozenTime为全局冻结时间
     */
    public int globalHostFrozenTime = 30;

    /**
     *   Host局部冻结时间，只会影响当前长传操作  单位：秒   默认：5*60  推荐范围：[60 ~ 10*60]
     *   当某个Host的上传失败后并且可能短时间可能会恢复，会冻结该Host，partialHostFrozenTime为全局冻结时间
     */
    public int partialHostFrozenTime = 5*60;


    private static GlobalConfiguration configuration = new GlobalConfiguration();
    private GlobalConfiguration(){
    }
    public static GlobalConfiguration getInstance(){
        return configuration;
    }
}
