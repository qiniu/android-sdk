package com.qiniu.android.http;

import android.os.Build;
import android.text.TextUtils;

import com.qiniu.android.common.Constants;
import com.qiniu.android.utils.StringUtils;

import java.nio.charset.Charset;
import java.util.Locale;
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

    static String getUserAgent(String id) {
        return format("QiniuAndroid/%s (%s; %s; %s", Constants.VERSION,
                osVersion(), device(), id);
    }

    private static String osVersion() {
        try {
            String v = android.os.Build.VERSION.RELEASE;
            if (v == null) {
                return "-";
            }
            return StringUtils.strip(v.trim());
        } catch (Throwable t) {
            return "-";
        }
    }

    private static String device() {
        try {
            String model = Build.MODEL.trim();
            String device = deviceName(Build.MANUFACTURER.trim(), model);
            if (TextUtils.isEmpty(device)) {
                device = deviceName(Build.BRAND.trim(), model);
            }
            return StringUtils.strip((device == null ? "-" : device) + "-" + model);
        } catch (Throwable t) {
            return "-";
        }
    }

    private static String deviceName(String manufacturer, String model) {
        String str = manufacturer.toLowerCase(Locale.getDefault());
        if ((str.startsWith("unknown")) || (str.startsWith("alps")) ||
                (str.startsWith("android")) || (str.startsWith("sprd")) ||
                (str.startsWith("spreadtrum")) || (str.startsWith("rockchip")) ||
                (str.startsWith("wondermedia")) || (str.startsWith("mtk")) ||
                (str.startsWith("mt65")) || (str.startsWith("nvidia")) ||
                (str.startsWith("brcm")) || (str.startsWith("marvell")) ||
                (model.toLowerCase(Locale.getDefault()).contains(str))) {
            return null;
        }
        return manufacturer;
    }

    public String getUa(String part) {
        String _part = ("" + part).trim();
        _part = _part.substring(0, Math.min(16, _part.length()));
        return new String((ua + "; " + _part + ")").getBytes(Charset.forName("ISO-8859-1")));
    }

}
