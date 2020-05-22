package com.qiniu.android.http.dns;


import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by jemy on 2019/9/23.
 */

public class DnsCacheKey {
    public String currentTime;
    public String localIp;
    public String akScope;

    public DnsCacheKey() {

    }

    public DnsCacheKey(String currentTime, String localIp, String akScope) {
        this.currentTime = currentTime;
        this.localIp = localIp;
        this.akScope = akScope;
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

    public void setAkScope(String akScope) {
        this.akScope = akScope;
    }

    public void setCurrentTime(String currentTime) {
        this.currentTime = currentTime;
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public static DnsCacheKey toCacheKey(String key) {
        try {
            JSONObject object = new JSONObject(key);
            return new DnsCacheKey(object.getString("currentTime"), object.getString("localIp"), object.getString("akScope"));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String toString() {
        return "{\"currentTime\":\"" + currentTime + "\", \"localIp\":\"" + localIp + "\", \"akScope\":\"" + akScope + "\"}";

    }
}
