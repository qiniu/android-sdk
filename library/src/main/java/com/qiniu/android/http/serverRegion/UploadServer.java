package com.qiniu.android.http.serverRegion;

import com.qiniu.android.http.request.IUploadServer;

/**
 * 上传服务
 *
 * @hidden
 */
public class UploadServer extends IUploadServer {

    private final String serverId;
    private final String host;
    private final String ip;
    private final String source;
    private final Long ipPrefetchedTime;
    private String httpVersion;

    /**
     * 构造函数
     *
     * @param serverId         server id
     * @param host             host
     * @param ip               ip
     * @param source           dns 解析源
     * @param ipPrefetchedTime dns 解析时间
     */
    public UploadServer(String serverId,
                        String host,
                        String ip,
                        String source,
                        Long ipPrefetchedTime) {
        this.serverId = serverId;
        this.host = host;
        this.ip = ip;
        this.source = source;
        this.ipPrefetchedTime = ipPrefetchedTime;
    }

    /**
     * 获取 server id
     *
     * @return server id
     */
    @Override
    public String getServerId() {
        return this.serverId;
    }

    /**
     * 获取 HTTP version
     *
     * @return HTTP version
     */
    @Override
    public String getHttpVersion() {
        return httpVersion;
    }

    /**
     * 配置 HTTP version
     *
     * @param httpVersion HTTP version
     */
    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    /**
     * 获取 IP
     *
     * @return IP
     */
    @Override
    public String getIp() {
        return ip;
    }

    /**
     * 获取 dns 解析源
     *
     * @return dns 解析源
     */
    @Override
    public String getSource() {
        return source;
    }

    /**
     * 获取 dns 解析时间
     *
     * @return dns 解析时间
     */
    @Override
    public Long getIpPrefetchedTime() {
        return ipPrefetchedTime;
    }

    /**
     * 获取服务域名
     *
     * @return 服务域名
     */
    @Override
    public String getHost() {
        return host;
    }
}
