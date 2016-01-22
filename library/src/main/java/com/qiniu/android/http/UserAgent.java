package com.qiniu.android.http;

import android.os.Build;
import android.text.TextUtils;

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
                android.os.Build.VERSION.RELEASE, device(), id);
    }

    private static String device() {
        String model = Build.MODEL.trim();
        String device = deviceName(Build.MANUFACTURER.trim(), model);
        if (TextUtils.isEmpty(device)) {
            device = deviceName(Build.BRAND.trim(), model);
        }
        return (device == null ? "" : device) + "-" + model;
    }

    private static String deviceName(String manufacturer, String model) {
        String str = manufacturer.toLowerCase();
        if ((str.startsWith("unknown")) || (str.startsWith("alps")) ||
                (str.startsWith("android")) || (str.startsWith("sprd")) ||
                (str.startsWith("spreadtrum")) || (str.startsWith("rockchip")) ||
                (str.startsWith("wondermedia")) || (str.startsWith("mtk")) ||
                (str.startsWith("mt65")) || (str.startsWith("nvidia")) ||
                (str.startsWith("brcm")) || (str.startsWith("marvell")) ||
                (model.toLowerCase().contains(str))) {
            return null;
        }
        return manufacturer;
    }

    public String toString() {
        return ua;
    }
}
