package com.qiniu.android.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Dns解析类
 */
public final class Dns {
    public static String[] getAddresses(String hostName){
        InetAddress[] ret = null;
        try {
            ret = InetAddress.getAllByName(hostName);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return new String[0];
        }
        String[] r = new String[ret.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = ret[i].getHostAddress();
        }
        return r;
    }

    public static String getAddressesString(String hostName){
        return StringUtils.join(getAddresses(hostName), ";");
    }

}
