package com.qiniu.android.http.custom;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jemy on 2019/9/23.
 */

public class DnsCacheInfo implements java.io.Serializable {
    public String currentTime;
    public String localIp;
    public String akScope;
    public ConcurrentHashMap<String, List<InetAddress>> info;

    public DnsCacheInfo() {}

    public DnsCacheInfo(String currentTime, String localIp, String akScope, ConcurrentHashMap<String, List<InetAddress>> info) {
        this.currentTime = currentTime;
        this.localIp = localIp;
        this.akScope = akScope;
        this.info = info;
    }

    public String getCurrentTime() {
        return currentTime;
    }

    public String getAkScope() {
        return akScope;
    }

    public String getLocalIp() {
        return localIp;
    }

    public ConcurrentHashMap<String, List<InetAddress>> getInfo() {
        return info;
    }

    public void setAkScope(String akScope) {
        this.akScope = akScope;
    }

    public void setCurrentTime(String currentTime) {
        this.currentTime = currentTime;
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public void setInfo(ConcurrentHashMap<String, List<InetAddress>> info) {
        this.info = info;
    }

    public String cacheKey(){
        return localIp;
    }

    @Override
    public String toString() {
        return "{\"currentTime\":\"" + currentTime + "\", \"localIp\":\"" + localIp + "\", \"akScope\":\"" + akScope + "\"}";
    }
}
