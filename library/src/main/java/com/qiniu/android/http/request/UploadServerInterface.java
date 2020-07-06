package com.qiniu.android.http.request;

import com.qiniu.android.http.dns.IDnsNetworkAddress;

import java.net.InetAddress;

public interface UploadServerInterface {

    String getServerId();

    IDnsNetworkAddress getNetworkAddress();

    String getIp();

    String getHost();
}
