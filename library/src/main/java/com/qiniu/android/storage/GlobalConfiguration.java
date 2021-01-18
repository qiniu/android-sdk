package com.qiniu.android.storage;

import com.qiniu.android.http.dns.Dns;
import com.qiniu.android.utils.Utils;

import java.util.List;

public class GlobalConfiguration {

    /**
     * 是否开启dns预解析 默认开启
     */
    public boolean isDnsOpen = true;

    /**
     * dns 预取失败后 会进行重新预取  dnsRepreHostNum为最多尝试次数
     */
    public int dnsRepreHostNum = 2;

    /**
     * dns预取缓存时间  单位：秒
     */
    public int dnsCacheTime = 120;

    /**
     * 自定义DNS解析客户端host
     */
    public Dns dns = null;

    /**
     * 自定义DNS解析客户端host
     */
    public String dnsCacheDir = Utils.sdkDirectory() + "/dnsCache/";

    /**
     * Host全局冻结时间  单位：秒   默认：30  推荐范围：[10 ~ 60]
     * 当某个Host的上传失败后并且可能短时间无法恢复，会冻结该Host，globalHostFrozenTime为全局冻结时间
     * Host全局冻结时间  单位：秒   默认：10  推荐范围：[5 ~ 30]
     * 当某个Host的上传失败后并且可能短时间无法恢复，会冻结该Host
     */
    public int globalHostFrozenTime = 10;

    /**
     * Host局部冻结时间，只会影响当前上传操作  单位：秒   默认：5*60  推荐范围：[60 ~ 10*60]
     * 当某个Host的上传失败后并且短时间可能会恢复，会局部冻结该Host
     */
    public int partialHostFrozenTime = 5 * 60;


    /**
     * 网络连接状态检测使用的connectCheckURLStrings，网络链接状态检测可能会影响重试机制，启动网络连接状态检测有助于提高上传可用性。
     * 当请求的 Response 为网络异常时，并发对 connectCheckURLStrings 中 URLString 进行 HEAD 请求，以此检测当前网络状态的链接状态，其中任意一个 URLString 链接成功则认为当前网络状态链接良好；
     * 当 connectCheckURLStrings 为 nil 或者 空数组时则弃用检测功能。
     */
    public String[] connectCheckURLStrings = new String[]{"http://www.baidu.com", "http://www.google.com"};

    /**
     * 网络连接状态检测HEAD请求超时，默认：3s
     */
    public int connectCheckTimeout = 3;

    private static GlobalConfiguration configuration = new GlobalConfiguration();

    private GlobalConfiguration() {
    }

    public static GlobalConfiguration getInstance() {
        return configuration;
    }
}
