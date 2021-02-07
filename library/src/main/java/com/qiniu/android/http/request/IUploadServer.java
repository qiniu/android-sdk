package com.qiniu.android.http.request;


public abstract class IUploadServer {
    public static String HttpVersion1 = "http_version_1";
    public static String HttpVersion2 = "http_version_2";
    public static String HttpVersion3 = "http_version_3";

    public boolean isHttp3() {
        String httpVersion = getHttpVersion();
        if (httpVersion == null) {
            return false;
        }
        return httpVersion.equals(IUploadServer.HttpVersion3);
    }

    public boolean isHttp2() {
        String httpVersion = getHttpVersion();
        if (httpVersion == null) {
            return false;
        }
        return httpVersion.equals(IUploadServer.HttpVersion2);
    }

    public abstract String getServerId();

    public abstract String getHttpVersion();

    public abstract String getHost();

    public abstract String getIp();

    public abstract String getSource();

    public abstract Long getIpPrefetchedTime();
}
