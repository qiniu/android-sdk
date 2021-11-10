package com.qiniu.android.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Process;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.TelephonyManager;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by bailong on 16/9/7.
 */
public final class AndroidNetwork {
    public static boolean isNetWorkReady() {
        Context c = ContextGetter.applicationContext();
        if (c == null) {
            return true;
        }
        ConnectivityManager connMgr = (ConnectivityManager)
                c.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            NetworkInfo info = connMgr.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 获取ip地址
     * 使用DNS解析某地址时，可能会同时返回IPv4和IPv6的地址。
     * 如果同时拥有IPv4和IPv6的地址，是会默认优先上报IPv6的地址
     *
     * @return
     */
    public static String getHostIP() {
        String hostIp = null;
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            if (nis == null) {
                return null;
            }

            InetAddress ia = null;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (!ia.isLinkLocalAddress() && !ia.isLoopbackAddress()) {
                        hostIp = ia.getHostAddress();
                        break;
                    }
                    continue;
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return hostIp;
    }

    public static String networkType(Context context) {
        try {
            return networkTypeWithException(context);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String networkTypeWithException(Context context) throws Exception {
        if (context == null){
            return Constants.NETWORK_CLASS_UNKNOWN;
        }

        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return Constants.NETWORK_CLASS_UNKNOWN;
        }

        NetworkInfo networkInfo = connectivity.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            return Constants.NETWORK_CLASS_UNKNOWN;
        }

        int netWorkType = networkInfo.getType();
        if (netWorkType == ConnectivityManager.TYPE_WIFI) {
            return Constants.NETWORK_WIFI;
        } else if (netWorkType == ConnectivityManager.TYPE_MOBILE) {
            return getNetWorkClass(context);
        }
        return Constants.NETWORK_CLASS_UNKNOWN;
    }

    private static String getNetWorkClass(Context context) {
        if (context.checkPermission(Manifest.permission.READ_PHONE_STATE, Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED) {
            return Constants.NETWORK_CLASS_UNKNOWN;
        }

        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null){
            return Constants.NETWORK_CLASS_UNKNOWN;
        }

        switch (telephonyManager.getNetworkType()) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return Constants.NETWORK_CLASS_2_G;

            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return Constants.NETWORK_CLASS_3_G;

            case TelephonyManager.NETWORK_TYPE_LTE:
                return Constants.NETWORK_CLASS_4_G;

            default:
                return Constants.NETWORK_CLASS_UNKNOWN;
        }
    }
}
