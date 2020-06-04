package com.qiniu.android.http.request;

import java.net.InetAddress;

public interface UploadServerInterface {

    String getServerId();

    InetAddress getInetAddress();

    String getIp();

    String getHost();
}
