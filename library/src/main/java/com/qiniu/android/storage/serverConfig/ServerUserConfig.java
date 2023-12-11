package com.qiniu.android.storage.serverConfig;

import com.qiniu.android.utils.Cache;
import com.qiniu.android.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * server user config
 *
 * @hidden
 */
public class ServerUserConfig implements Cache.Object {

    private long timestamp;
    private long ttl = 10;
    private Boolean http3Enable;
    private Boolean networkCheckEnable;

    private JSONObject info;

    /**
     * 构造函数
     *
     * @param info json 数据
     */
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

    /**
     * 获取 json 数据
     *
     * @return json 数据
     */
    @Override
    public JSONObject toJson() {
        return info;
    }

    /**
     * HTTP/3 是否开启
     *
     * @return HTTP/3 是否开启
     */
    public Boolean getHttp3Enable() {
        return http3Enable;
    }

    /**
     * 网络检测是否开启
     *
     * @return 网络检测是否开启
     */
    public Boolean getNetworkCheckEnable() {
        return networkCheckEnable;
    }

    /**
     * 获取 json 信息
     *
     * @return json 信息
     */
    public JSONObject getInfo() {
        return toJson();
    }

    /**
     * 配置是否有效
     *
     * @return 配置是否有效
     */
    public boolean isValid() {
        return Utils.currentSecondTimestamp() < (this.timestamp + this.ttl);
    }
}
