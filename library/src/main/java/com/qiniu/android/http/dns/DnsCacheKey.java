package com.qiniu.android.http.dns;


import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by jemy on 2019/9/23.
 */

class DnsCacheKey {
    private String currentTime;
    private String localIp;
    private String akScope;

    DnsCacheKey(String currentTime, String localIp) {
        this.currentTime = currentTime;
        this.localIp = localIp;
    }

    DnsCacheKey(String currentTime, String localIp, String akScope) {
        this.currentTime = currentTime;
        this.localIp = localIp;
        this.akScope = akScope;
    }

    String getCurrentTime() {
        return currentTime;
    }

    String getAkScope() {
        return akScope;
    }

    String getLocalIp() {
        return localIp;
    }

    void setAkScope(String akScope) {
        this.akScope = akScope;
    }

    void setCurrentTime(String currentTime) {
        this.currentTime = currentTime;
    }

    void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    static DnsCacheKey toCacheKey(String key) {
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
