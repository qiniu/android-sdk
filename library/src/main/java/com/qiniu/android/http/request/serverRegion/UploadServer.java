package com.qiniu.android.http.request.serverRegion;

import com.qiniu.android.http.request.UploadServerInterface;

import java.net.InetAddress;

public class UploadServer implements UploadServerInterface {

    private final String serverId;
    private final String host;
    private final InetAddress inetAddress;

    public UploadServer(String serverId, String host, InetAddress inetAddress) {
        this.serverId = serverId;
        this.host = host;
        this.inetAddress = inetAddress;
    }

    @Override
    public String getServerId() {
        return this.serverId;
    }

    @Override
    public InetAddress getInetAddress() {
        return inetAddress;
    }

    @Override
    public String getIp() {
        if (inetAddress == null){
            return null;
        } else {
            return inetAddress.getHostAddress();
        }
    }

    @Override
    public String getHost() {
        return host;
    }
}
