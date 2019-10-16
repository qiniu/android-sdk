package com.qiniu.android.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

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

}
