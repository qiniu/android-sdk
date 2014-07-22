package com.qiniu.conf;

import java.util.Random;

public class Conf {
    public static final String VERSION = "6.0.5";
    public static String UP_HOST = "http://upload.qiniu.com";
    public static String UP_HOST2 = "http://up.qiniu.com";

    private static String id = genId();

    public static String getUserAgent() {
        return  "QiniuAndroid/" + VERSION + " (" + android.os.Build.VERSION.RELEASE + "; "
            + android.os.Build.MODEL+ "; " + id +")";
    }

    private static String genId(){
        Random r = new Random();
        int rnum = r.nextInt(999);
        return System.currentTimeMillis() + "" + rnum;
    }
}
