package com.qiniu.android.http.request.serverRegion;

import com.qiniu.android.http.request.UploadServerInterface;

public class UploadServer implements UploadServerInterface {

    private final String serverId;
    private final String host;
    private final String ip;

    public UploadServer(String serverId, String host, String ip) {
        this.serverId = serverId;
        this.host = host;
        this.ip = ip;
    }

    @Override
    public String getServerId() {
        return this.serverId;
    }

    @Override
    public String getIp() {
        return this.ip;
    }

    @Override
    public String getHost() {
        return this.host;
    }
}
