package com.qiniu.android.http.serverRegion;

import com.qiniu.android.http.request.IUploadServer;

public class UploadServer implements IUploadServer {

    private final String serverId;
    private final String host;
    private final String ip;
    private final String source;
    private final Long ipPrefetchedTime;

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

    @Override
    public String getServerId() {
        return this.serverId;
    }

    @Override
    public String getIp() {
        return ip;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public Long getIpPrefetchedTime() {
        return ipPrefetchedTime;
    }

    @Override
    public String getHost() {
        return host;
    }
}
