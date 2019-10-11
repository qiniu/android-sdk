package com.qiniu.android.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.net.Inet6Address;
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
        String ipv6 = null;
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia = null;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        //ipv6 = checkIpv6(ia.getHostAddress());
                        if (!ia.isLinkLocalAddress()&&!ia.isLoopbackAddress()) {
                            hostIp = ia.getHostAddress();
                            break;
                        }
                        continue;
                    }
                    String ip = ia.getHostAddress();
                    if (!"127.0.0.1".equals(ip)) {
                        hostIp = ia.getHostAddress();
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return hostIp;
    }

    /**
     * first segment contains "fe" or "fc": https://blog.csdn.net/fdl19881/article/details/7091138
     *
     * @param ipv6
     * @return
     */
    private static String checkIpv6(String ipv6) {
        if (ipv6 != null) {
            String[] split = ipv6.split("%");
            String s1 = split[0];
            if (s1 != null && s1.contains(":")) {
                String[] split1 = s1.split(":");
                if (split1.length == 6 || split1.length == 8) {
                    if (split1[0].contains("fe") || split1[0].contains("fc")) {
                        return null;
                    } else {
                        return s1;
                    }
                }
            }
        }
        return null;
    }
}
