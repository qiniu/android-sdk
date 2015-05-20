package com.qiniu.android.storage;


public final class Zone {
    public final String upHost;
    public final String upHostBackup;
    public final String upIp;

    public Zone(String upHost, String upHostBackup, String upIp) {
        this.upHost = upHost;
        this.upHostBackup = upHostBackup;
        this.upIp = upIp;
    }

    public static final Zone zone0 =
            new Zone("upload.qiniu.com", "up.qiniu.com", "183.136.139.16");


    public static final Zone zone1 =
            new Zone("upload-z1.qiniu.com", "up-z1.qiniu.com", "106.38.227.27");

}
