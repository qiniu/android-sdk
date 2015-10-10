package com.qiniu.android.common;

/**
 * Created by bailong on 15/10/10.
 */
public final class Zone {
    public static final Zone zone0 =
            createZone("upload.qiniu.com", "up.qiniu.com", "183.136.139.10", "115.231.182.136");
    public static final Zone zone1 =
            createZone("upload-z1.qiniu.com", "up-z1.qiniu.com", "106.38.227.27", "106.38.227.28");
    public final ServiceAddress up;
    public final ServiceAddress upBackup;

    public Zone(ServiceAddress up, ServiceAddress upBackup) {
        this.up = up;
        this.upBackup = upBackup;
    }

    private static Zone createZone(String upHost, String upHostBackup, String upIp, String upIp2) {
        String[] upIps = {upIp, upIp2};
        ServiceAddress up = new ServiceAddress("http://" + upHost, upIps);
        ServiceAddress upBackup = new ServiceAddress("http://" + upHostBackup, upIps);
        return new Zone(up, upBackup);
    }

}
