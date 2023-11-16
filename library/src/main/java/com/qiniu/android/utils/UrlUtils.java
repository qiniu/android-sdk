package com.qiniu.android.utils;

public class UrlUtils {

    public static String removeHostScheme(String host) {
        if (host == null || StringUtils.isNullOrEmpty(host)) {
            return null;
        }

        host = host.replace("http://", "");
        host = host.replace("https://", "");
        return host;
    }


    /**
     * 如果 host 包含 scheme 则优先使用 host 中包含的 scheme
     * 如果 host 不包含 scheme 则按照 useHttps 增加 scheme
     */
    public static String setHostScheme(String host, boolean useHttps) {
        if (host == null || StringUtils.isNullOrEmpty(host)) {
            return null;
        }

        if (host.startsWith("http://") || host.startsWith("https://") ) {
            return host;
        }

        return (useHttps ? "https://" : "http://") + host;
    }
}
