package com.qiniu.android.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.text.TextUtils;

import com.qiniu.android.common.Constants;

import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * utils
 */
public class Utils {

    private static Boolean isDebug = null;

    private Utils() {
    }

    /**
     * 是否是 Debug
     *
     * @return 是否是 Debug
     */
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

    /**
     * SDK Version
     *
     * @return SDK Version
     */
    public static String sdkVerion() {
        return Constants.VERSION;
    }

    /**
     * SDK Language
     *
     * @return SDK Language
     */
    public static String sdkLanguage() {
        return "Android";
    }

    /**
     * 获取 PID
     *
     * @return PID
     * @hidden
     */
    public static Integer getCurrentProcessID() {
        return android.os.Process.myPid();
    }

    /**
     * 获取 TID
     *
     * @return TID
     * @hidden
     */
    public static Long getCurrentThreadID() {
        Thread thread = Thread.currentThread();
        return thread.getId();
    }

    /**
     * 获取系统名
     *
     * @return 系统名
     * @hidden
     */
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

    /**
     * 获取系统版本
     *
     * @return 系统版本
     * @hidden
     */
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

    /**
     * 获取信号强度
     *
     * @return 信号强度
     * @hidden
     */
    @Deprecated
    public static Integer getCurrentSignalStrength() {
        return null;
    }

    /**
     * 获取网络类型
     *
     * @return 网络类型
     * @hidden
     */
    public static String getCurrentNetworkType() {
        Context context = ContextGetter.applicationContext();
        if (context == null) {
            return "";
        }
        return AndroidNetwork.networkType(context);
    }

    /**
     * 获取当前时间戳
     * 单位：毫秒
     *
     * @return 当前时间戳
     * @hidden
     */
    public static long currentTimestamp() {
        return new Date().getTime();
    }

    /**
     * 获取当前时间戳
     * 单位：秒
     *
     * @return 当前时间戳
     * @hidden
     */
    public static long currentSecondTimestamp() {
        return currentTimestamp() / 1000;
    }

    /**
     * 两个时间的时间间隔 单位：毫秒
     *
     * @param startDate 开始时间
     * @param endDate   结束时间
     * @return 时间间隔
     * @hidden
     */
    public static long dateDuration(Date startDate, Date endDate) {
        if (startDate != null && endDate != null) {
            return (endDate.getTime() - startDate.getTime());
        } else {
            return 0l;
        }
    }

    /**
     * 计算 上传 或 下载 速度 单位：B/s
     *
     * @param bytes     单位： B
     * @param totalTime 单位：ms
     * @return 速度
     * @hidden
     */
    public static Long calculateSpeed(Long bytes, Long totalTime) {
        if (bytes == null || bytes < 0 || totalTime == null || totalTime == 0) {
            return null;
        }
        return bytes * 1000 / totalTime;
    }

    /**
     * SDK 路径
     *
     * @return SDK 路径
     */
    public static String sdkDirectory() {
        Context context = ContextGetter.applicationContext();
        if (context == null) {
            return null;
        }
        String directory = context.getCacheDir().getAbsolutePath() + "/qiniu";
        return directory;
    }

    /**
     * Escape 字符串
     *
     * @param string 字符串
     * @return Escape 后的字符串
     * @hidden
     */
    public static String formEscape(String string) {
        if (string == null) {
            return null;
        }
        String ret = string;
        ret = ret.replace("\\", "\\\\");
        ret = ret.replace("\"", "\\\"");
        return ret;
    }

    /**
     * 获取 ip 类型
     *
     * @param ip   ip
     * @param host host
     * @return ip 类型
     * @hidden
     */
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

    /**
     * 获取 ip 类型
     *
     * @param httpVersion HTTP version
     * @param ip          ip
     * @param host        host
     * @return ip 类型
     * @hidden
     */
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

    /**
     * 判断 IP 是否为 IPv6
     *
     * @param ip ip
     * @return 是否为 IPv6
     * @hidden
     */
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
