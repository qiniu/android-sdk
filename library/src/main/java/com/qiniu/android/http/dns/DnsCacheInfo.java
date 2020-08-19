package com.qiniu.android.http.dns;

import com.qiniu.android.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    public ConcurrentHashMap<String, List<IDnsNetworkAddress>> info;

    public static DnsCacheInfo createDnsCacheInfoByData(byte[] jsonData) {
        if (jsonData == null){
            return null;
        }
        Object dnsCacheInfo = StringUtils.toObject(jsonData);
        if (dnsCacheInfo instanceof DnsCacheInfo){
            return (DnsCacheInfo)dnsCacheInfo;
        } else {
            return null;
        }
    }

    public DnsCacheInfo() {
    }

    public DnsCacheInfo(String currentTime, String localIp, ConcurrentHashMap<String, List<IDnsNetworkAddress>> info) {
        this.currentTime = currentTime;
        this.localIp = localIp;
        this.info = info;
    }

    String getCurrentTime() {
        return currentTime;
    }

    String getLocalIp() {
        return localIp;
    }

    public ConcurrentHashMap<String, List<IDnsNetworkAddress>> getInfo() {
        return info;
    }

    void setCurrentTime(String currentTime) {
        this.currentTime = currentTime;
    }

    void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public void setInfo(ConcurrentHashMap<String, List<IDnsNetworkAddress>> info) {
        this.info = info;
    }

    public String cacheKey(){
        return localIp;
    }

    public byte[] toJsonData(){
        return StringUtils.toByteArray(this);
    }

    @Override
    public String toString() {
        return "{\"currentTime\":\"" + currentTime + "\", \"localIp\":\"" + localIp + "\"}";
    }
}
