package com.qiniu.conf;

public class Conf {
	private static final String USER_AGENT_PREFIX = "QiniuAndroid/";
    public static final String VERSION = "6.0.4";
	public static final String UP_HOST = "http://upload.qiniu.com";
    public static final String UP_HOST2 = "http://up.qbox.me";

    public static String getUserAgent() {
        return  USER_AGENT_PREFIX + VERSION + " (" + android.os.Build.VERSION.RELEASE + "; " + android.os.Build.MODEL+ ")";
    }
}
