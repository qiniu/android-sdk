package com.qiniu.android.http.dns;


/**
 * Dns 预解析信息
 */
public interface IDnsNetworkAddress {

    /**
     * 预解析的域名
     *
     * @return 预解析的域名
     */
    String getHostValue();

    /**
     * 预解析域名的 IP 信息
     *
     * @return 预解析域名的 IP 信息
     */
    String getIpValue();

    /**
     * 预解析域名的 IP 有效时间 单位：秒
     *
     * @return 预解析域名的 IP 有效时间
     */
    Long getTtlValue();

    /**
     * 预解析的源，自定义dns返回 "customized"
     *
     * @return 预解析的源
     */
    String getSourceValue();

    /**
     * 解析到 host 时的时间戳，单位：秒
     *
     * @return 解析到 host 时的时间戳
     */
    Long getTimestampValue();
}
