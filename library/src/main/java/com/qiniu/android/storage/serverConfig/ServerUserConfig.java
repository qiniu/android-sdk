package com.qiniu.android.storage.serverConfig;

import com.qiniu.android.utils.Utils;

import org.json.JSONObject;

class ServerUserConfig {

    private long timestamp;
    private long ttl = 10;
    private Boolean http3Enable;
    private Long retryMax;
    private Boolean networkCheckEnable;

    private JSONObject info;

    ServerUserConfig(JSONObject info) {
        this.timestamp = Utils.currentSecondTimestamp();

        if (info == null) {
            return;
        }
        this.info = info;

        JSONObject http3 = info.optJSONObject("http3");
        if (http3 != null && http3.opt("enabled") != null) {
            this.http3Enable = http3.optBoolean("enabled");
        }

        if (info.opt("retryMax") != null) {
            this.retryMax = info.optLong("retryMax", 1);
        }

        JSONObject networkCheck = info.optJSONObject("network_check");
        if (networkCheck != null && networkCheck.opt("enabled") != null) {
            this.networkCheckEnable = networkCheck.optBoolean("enabled");
        }
    }

    public Boolean getHttp3Enable() {
        return http3Enable;
    }

    public Long getRetryMax() {
        return retryMax;
    }

    public Boolean getNetworkCheckEnable() {
        return networkCheckEnable;
    }

    public JSONObject getInfo() {
        return info;
    }

    boolean isValid() {
        return Utils.currentSecondTimestamp() < (this.timestamp + this.ttl);
    }
}
