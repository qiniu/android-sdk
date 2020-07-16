package com.qiniu.android.http.request;


public interface IUploadServer {

    String getServerId();

    String getHost();

    String getIp();

    String getSource();

    Long getIpPrefetchedTime();
}
