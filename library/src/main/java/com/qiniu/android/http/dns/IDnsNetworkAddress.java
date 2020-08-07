package com.qiniu.android.http.dns;


public interface IDnsNetworkAddress {
    /// 域名
    String getHostValue();

    /// 地址IP信息
    String getIpValue();

    /// ip有效时间 单位：秒
    Long getTtlValue();

    /// ip预取来源, 自定义dns返回 "customized"
    String getSourceValue();

    /// 解析到host时的时间戳 单位：秒
    Long getTimestampValue();
}
