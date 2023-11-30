package com.qiniu.android.http.request;


import java.net.InetAddress;

/**
 * upload server 信息
 *
 * @hidden
 */
public abstract class IUploadServer {

    /**
     * HTTP/1.1
     */
    public static String HttpVersion1 = "http_version_1";

    /**
     * HTTP/2
     */
    public static String HttpVersion2 = "http_version_2";

    /**
     * HTTP/3
     */
    public static String HttpVersion3 = "http_version_3";

    /**
     * 构造函数
     */
    protected IUploadServer() {
    }

    /**
     * 是否使用 HTTP/3
     *
     * @return 是否使用 HTTP/3
     */
    public boolean isHttp3() {
        String httpVersion = getHttpVersion();
        if (httpVersion == null) {
            return false;
        }
        return httpVersion.equals(IUploadServer.HttpVersion3);
    }

    /**
     * 是否使用 HTTP/2
     *
     * @return 是否使用 HTTP/2
     */
    public boolean isHttp2() {
        String httpVersion = getHttpVersion();
        if (httpVersion == null) {
            return false;
        }
        return httpVersion.equals(IUploadServer.HttpVersion2);
    }

    /**
     * 获取 ServerId
     *
     * @return ServerId
     */
    public abstract String getServerId();

    /**
     * 获取 HttpVersion
     *
     * @return HttpVersion
     */
    public abstract String getHttpVersion();

    /**
     * 获取 Host
     *
     * @return Host
     */
    public abstract String getHost();

    /**
     * 获取 IP
     *
     * @return IP
     */
    public abstract String getIp();

    /**
     * 获取 DNS 解析 Source
     *
     * @return DNS 解析 Source
     */
    public abstract String getSource();

    /**
     * 获取 DNS 解析时间戳
     *
     * @return DNS 解析时间戳
     */
    public abstract Long getIpPrefetchedTime();

    /**
     * 获取 DNS 解析信息
     *
     * @return DNS 解析信息
     */
    public InetAddress getInetAddress() {
        String ip = getIp();
        String host = getHost();
        if (getHost() == null || ip == null || ip.length() == 0) {
            return null;
        }

        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            return InetAddress.getByAddress(host, ipAddress.getAddress());
        } catch (Exception e) {
            return null;
        }
    }
}
