package com.qiniu.android.http;

import com.qiniu.android.common.Constants;

import java.util.Random;

import static java.lang.String.format;

/**
 * Created by bailong on 15/6/23.
 */
public final class UserAgent {
    private static UserAgent _instance = new UserAgent();
    public final String id;
    public final String ua;

    private UserAgent() {
        id = genId();
        ua = getUserAgent(id);
    }

    public static UserAgent instance() {
        return _instance;
    }

    private static String genId() {
        Random r = new Random();
        return System.currentTimeMillis() + "" + r.nextInt(999);
    }

    private static String getUserAgent(String id) {
        return format("QiniuAndroid/%s (%s; %s; %s)", Constants.VERSION,
                android.os.Build.VERSION.RELEASE, android.os.Build.MODEL, id);
    }

    public String toString() {
        return ua;
    }
}
