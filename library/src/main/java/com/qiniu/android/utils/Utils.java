package com.qiniu.android.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.text.TextUtils;

import com.qiniu.android.common.Constants;

import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class Utils {

    private static Boolean isDebug = null;
    public static boolean isDebug() {
        if (isDebug != null) {
            return isDebug;
        }

        Context context = ContextGetter.applicationContext();
        if (context == null) {
            return false;
        }

        try {
            ApplicationInfo info = context.getApplicationInfo();
            isDebug = (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            return isDebug;
        } catch (Exception e) {
            return false;
        }
    }

    public static String sdkVerion() {
        return Constants.VERSION;
    }

    public static String sdkLanguage() {
        return "Android";
    }

    public static Integer getCurrentProcessID() {
        return android.os.Process.myPid();
    }

    public static Long getCurrentThreadID() {
        Thread thread = Thread.currentThread();
        return thread.getId();
    }

    public static String systemName() {
        try {
            String model = android.os.Build.MODEL != null ? android.os.Build.MODEL.trim() : "";
            String device = deviceName(Build.MANUFACTURER.trim(), model);
            if (TextUtils.isEmpty(device)) {
                device = deviceName(Build.BRAND.trim(), model);
            }
            String sdkVersion = android.os.Build.VERSION.SDK != null ? android.os.Build.VERSION.SDK : "";
            return device + "/" + model + "/" + sdkVersion;
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

    public static String systemVersion() {
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

    public static Integer getCurrentSignalStrength() {
        return null;
    }

    public static String getCurrentNetworkType() {
        Context context = ContextGetter.applicationContext();
        if (context == null) {
            return "";
        }
        return AndroidNetwork.networkType(context);
    }

    /// 单位：毫秒
    public static long currentTimestamp() {
        return new Date().getTime();
    }

    // 单位：秒
    public static long currentSecondTimestamp() {
        return currentTimestamp() / 1000;
    }

    /// 两个时间的时间段 单位：毫秒
    public static long dateDuration(Date startDate, Date endDate){
        if (startDate != null && endDate != null){
            return (endDate.getTime() - startDate.getTime());
        } else {
            return 0l;
        }
    }

    /**
     * 计算 上传 或 下载 速度 单位：B/s
     * @param bytes 单位： B
     * @param totalTime  单位：ms
     * @return 速度
     */
    public static Long calculateSpeed(Long bytes, Long totalTime){
        if (bytes == null || bytes < 0 || totalTime == null || totalTime == 0) {
            return null;
        }
        return bytes * 1000 / totalTime;
    }

    public static String sdkDirectory() {
        Context context = ContextGetter.applicationContext();
        if (context == null) {
            return null;
        }
        String directory = context.getCacheDir().getAbsolutePath() + "/qiniu";
        return directory;
    }

    public static String formEscape(String string) {
        if (string == null) {
            return null;
        }
        String ret = string;
        ret = ret.replace("\\", "\\\\");
        ret = ret.replace("\"", "\\\"");
        return ret;
    }

    @Deprecated
    public static String getIpType(String ip, String host) {
        String type = host;
        if (ip == null || ip.length() == 0) {
            return type;
        }
        if (ip.contains(":")) {
            type = getIPV6StringType(ip, host);
        } else if (ip.contains(".")) {
            type = getIPV4StringType(ip, host);
        }
        return type;
    }

    public static String getIpType(String httpVersion, String ip, String host) {
        if (httpVersion == null) {
            httpVersion = "";
        }

        String type = host;
        if (ip == null || ip.length() == 0) {
            return httpVersion + "-" + type;
        }
        if (ip.contains(":")) {
            type = getIPV6StringType(ip, host);
        } else if (ip.contains(".")) {
            type = getIPV4StringType(ip, host);
        }
        return httpVersion + "-" + type;
    }

    public static boolean isIpv6(String ip) {
        if (StringUtils.isNullOrEmpty(ip)) {
            return false;
        }

        return IPAddressUtil.isIPv6LiteralAddress(ip);
    }

    private static String getIPV4StringType(String ipv4String, String host) {
        if (host == null) {
            host = "";
        }

        String type = null;
        String[] ipNumberStrings = ipv4String.split("\\.");
        if (ipNumberStrings.length == 4) {
            int firstNumber = Integer.parseInt(ipNumberStrings[0]);
            int secondNumber = Integer.parseInt(ipNumberStrings[1]);
            type = firstNumber + "-" + secondNumber;
        }
        type = host + "-" + type;
        return type;
    }

    private static String getIPV6StringType(String ipv6String, String host) {
        if (host == null) {
            host = "";
        }

        String[] ipNumberStrings = ipv6String.split(":");
        String[] ipNumberStringsReal = new String[]{"0000", "0000", "0000", "0000", "0000", "0000", "0000", "0000"};
        String[] suppleStrings = new String[]{"0000", "000", "00", "0", ""};
        int i = 0;
        while (i < ipNumberStrings.length) {
            String ipNumberString = ipNumberStrings[i];
            if (ipNumberString.length() > 0) {
                ipNumberString = suppleStrings[ipNumberString.length()] + ipNumberString;
                ipNumberStringsReal[i] = ipNumberString;
            } else {
                break;
            }
            i++;
        }

        int j = ipNumberStrings.length - 1;
        int indexReal = ipNumberStringsReal.length - 1;
        while (i < j) {
            String ipNumberString = ipNumberStrings[j];
            if (ipNumberString.length() > 0) {
                ipNumberString = suppleStrings[ipNumberString.length()] + ipNumberString;
                ipNumberStringsReal[indexReal] = ipNumberString;
            } else {
                break;
            }
            j--;
            indexReal--;
        }
        String[] typeNumberArray = Arrays.copyOfRange(ipNumberStringsReal, 0, 4);
        String numberInfo = StringUtils.join(typeNumberArray, "-");
        return host + "-ipv6-" + numberInfo;
    }
}
