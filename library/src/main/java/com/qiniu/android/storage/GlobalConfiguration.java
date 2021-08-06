package com.qiniu.android.storage;

import android.content.Context;

import com.qiniu.android.http.dns.Dns;
import com.qiniu.android.utils.Utils;

public class GlobalConfiguration {

    /**
     * APP Context
     */
    public static Context appContext;

    /**
     * 是否开启dns预解析 默认开启
     */
    public boolean isDnsOpen = true;

    /**
     * dns 预取失败后 会进行重新预取  dnsRepreHostNum为最多尝试次数
     */
    public int dnsRepreHostNum = 2;

    /**
     * dns 预取, ip 默认有效时间  单位：秒 默认：120
     * 只有在 dns 预取未返回 ttl 时使用
     */
    public int dnsCacheTime = 120;

    /**
     * dns预取缓存最大有效时间  单位：秒 默认 1800
     * 当 dns 缓存 ip 过期并未刷新时，只要在 dnsCacheMaxTTL 时间内仍有效。
     */
    public int dnsCacheMaxTTL = 1800;

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
     * 额外指定的 dns 预解析 server，当使用系统 dns 预解析失败时会使用 dnsServers 的顺序逐个进行预解析
     */
    public String[] dnsServers = new String[]{"114.114.114.114", "8.8.8.8", "119.29.29.29", "208.67.222.222", "223.5.5.5", "1.1.1.1"};

    /**
     * 额外指定的 {@link GlobalConfiguration#dnsServers} 在 dns 预取时的超时时间
     * 单位：秒
     */
    public int dnsServerResolveTimeout = 2;

    /**
     * 网络连接状态检测使用的connectCheckURLStrings，网络链接状态检测可能会影响重试机制，启动网络连接状态检测有助于提高上传可用性。
     * 当请求的 Response 为网络异常时，并发对 connectCheckURLStrings 中 URLString 进行 HEAD 请求，以此检测当前网络状态的链接状态，其中任意一个 URLString 链接成功则认为当前网络状态链接良好；
     * 当 connectCheckURLStrings 为 nil 或者 空数组时则弃用检测功能。
     */
    public String[] connectCheckURLStrings = new String[]{"https://www.qiniu.com", "https://www.baidu.com", "https://www.google.com"};

    /**
     * 网络连接状态检测HEAD请求超时，默认：3s
     */
    public int connectCheckTimeout = 3;

    /**
     *  是否开启网络连接状态检测，默认：开启
     */
    public boolean connectCheckEnable = true;

    private static GlobalConfiguration configuration = new GlobalConfiguration();

    private GlobalConfiguration() {
    }

    public static GlobalConfiguration getInstance() {
        return configuration;
    }
}
