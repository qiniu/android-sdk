package com.qiniu.android.storage.serverConfig;

import com.qiniu.android.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

public class ServerUserConfig {

    private long timestamp;
    private long ttl = 10;
    private Boolean http3Enable;
    private Boolean networkCheckEnable;

    private JSONObject info;

    public ServerUserConfig(JSONObject info) {
        if (info == null) {
            return;
        }
        this.info = info;

        this.ttl = info.optLong("ttl", 5 * 60);

        if (info.opt("timestamp") != null) {
            this.timestamp = info.optLong("timestamp");
        }
        if (this.timestamp == 0) {
            this.timestamp = Utils.currentSecondTimestamp();
            try {
                info.putOpt("timestamp", this.timestamp);
            } catch (JSONException ignored) {
            }
        }

        JSONObject http3 = info.optJSONObject("http3");
        if (http3 != null && http3.opt("enabled") != null) {
            this.http3Enable = http3.optBoolean("enabled");
        }

        JSONObject networkCheck = info.optJSONObject("network_check");
        if (networkCheck != null && networkCheck.opt("enabled") != null) {
            this.networkCheckEnable = networkCheck.optBoolean("enabled");
        }
    }

    public Boolean getHttp3Enable() {
        return http3Enable;
    }

    public Boolean getNetworkCheckEnable() {
        return networkCheckEnable;
    }

    public JSONObject getInfo() {
        return info;
    }

    public boolean isValid() {
        return Utils.currentSecondTimestamp() < (this.timestamp + this.ttl);
    }
}
