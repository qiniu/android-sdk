package com.qiniu.android.http.request.serverRegion;

import com.qiniu.android.http.dns.IDnsNetworkAddress;
import com.qiniu.android.http.request.UploadServerInterface;

import java.net.InetAddress;

public class UploadServer implements UploadServerInterface {

    private final String serverId;
    private final String host;
    private final IDnsNetworkAddress networkAddress;

    public UploadServer(String serverId, String host, IDnsNetworkAddress networkAddress) {
        this.serverId = serverId;
        this.host = host;
        this.networkAddress = networkAddress;
    }

    @Override
    public String getServerId() {
        return this.serverId;
    }

    @Override
    public IDnsNetworkAddress getNetworkAddress() {
        return networkAddress;
    }

    @Override
    public String getIp() {
        if (networkAddress == null){
            return null;
        } else {
            return networkAddress.getIpValue();
        }
    }

    @Override
    public String getHost() {
        return host;
    }
}
