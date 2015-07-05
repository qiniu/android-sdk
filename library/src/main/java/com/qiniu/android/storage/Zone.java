package com.qiniu.android.storage;


public final class Zone {
    public static final Zone zone0 =
            new Zone("upload.qiniu.com", "up.qiniu.com", "183.136.139.10", "115.231.182.136");
    public static final Zone zone1 =
            new Zone("upload-z1.qiniu.com", "up-z1.qiniu.com", "106.38.227.27", "106.38.227.28");
    public final String upHost;
    public final String upHostBackup;
    public final String upIp;
    public final String upIp2;


    public Zone(String upHost, String upHostBackup, String upIp, String upIp2) {
        this.upHost = upHost;
        this.upHostBackup = upHostBackup;
        this.upIp = upIp;
        this.upIp2 = upIp2;
    }

}
