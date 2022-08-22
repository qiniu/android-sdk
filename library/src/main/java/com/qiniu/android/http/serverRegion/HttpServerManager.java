package com.qiniu.android.http.serverRegion;

import com.qiniu.android.utils.Utils;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public class HttpServerManager {
    private ConcurrentHashMap<String, Long> serversInfo = new ConcurrentHashMap<>();
    private final static HttpServerManager manager = new HttpServerManager();

    public static HttpServerManager getInstance() {
        return manager;
    }

    /**
     * 添加支持 http3 的 Host/ip 组合
     * @param host 支持 http3 的 Host
     * @param ip 支持 http3 Host 对应的 ip
     * @param liveDuration 有效时间 单位: 秒
     */
    public boolean addHttp3Server(String host, String ip, int liveDuration) {
        if (host == null || host.length() == 0 || ip == null || ip.length() == 0 || liveDuration < 0) {
            return false;
        }

        String type = getServerType(host, ip);
        long deadline = Utils.currentSecondTimestamp() + liveDuration;
        serversInfo.put(type, deadline);
        return true;
    }

    /**
     * Host/ip 组合是否支持 http3
     * @param host host
     * @param ip ip
     */
    public boolean isServerSupportHttp3(String host, String ip) {
        if (host == null || host.length() == 0 || ip == null || ip.length() == 0) {
            return false;
        }

        boolean support = false;
        String type = getServerType(host, ip);
        Long deadline = serversInfo.get(type);
        if (deadline != null && deadline > Utils.currentSecondTimestamp()) {
            support = true;
        }
        return support;
    }

    private String getServerType(String host, String ip) {
        return String.format(Locale.ENGLISH, "%s:%s", host, ip);
    }
}
