package com.qiniu.conf;

public class Conf {
    public static final String VERSION = "6.0.4";
    public static String UP_HOST = "http://upload.qiniu.com";
    public static String UP_HOST2 = "http://up.qbox.me";

    public static String getUserAgent() {
        return  "QiniuAndroid/" + VERSION + " (" + android.os.Build.VERSION.RELEASE + "; " + android.os.Build.MODEL+ ")";
    }
}
