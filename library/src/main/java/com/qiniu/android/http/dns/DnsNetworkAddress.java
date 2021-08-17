package com.qiniu.android.http.dns;

import com.qiniu.android.storage.GlobalConfiguration;
import com.qiniu.android.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

class DnsNetworkAddress implements IDnsNetworkAddress, java.io.Serializable {

    private final String hostValue;
    private final String ipValue;
    private final Long ttlValue;
    private final String sourceValue;
    private final Long timestampValue;

    static DnsNetworkAddress address(JSONObject jsonObject){
        String hostValue = null;
        String ipValue = null;
        Long ttlValue = null;
        String sourceValue = null;
        Long timestampValue = null;
        try {
            hostValue = jsonObject.getString("hostValue");
        } catch (JSONException e) {}
        try {
            ipValue = jsonObject.getString("ipValue");
        } catch (JSONException e) {}
        try {
            ttlValue = jsonObject.getLong("ttlValue");
        } catch (JSONException e) {}
        try {
            timestampValue = jsonObject.getLong("timestampValue");
        } catch (JSONException e) {}
        try {
            sourceValue = jsonObject.getString("sourceValue");
        } catch (JSONException e) {}

        DnsNetworkAddress networkAddress = new DnsNetworkAddress(hostValue, ipValue, ttlValue, sourceValue, timestampValue);
        return networkAddress;
    }

    DnsNetworkAddress(String hostValue,
                      String ipValue,
                      Long ttlValue,
                      String sourceValue,
                      Long timestampValue) {
        this.hostValue = hostValue;
        this.ipValue = ipValue;
        this.ttlValue = ttlValue;
        this.sourceValue = sourceValue;
        this.timestampValue = timestampValue;
    }

    JSONObject toJson(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("hostValue", this.hostValue);
        } catch (JSONException e) {}
        try {
            jsonObject.put("ipValue", this.ipValue);
        } catch (JSONException e) {}
        try {
            jsonObject.put("ttlValue", this.ttlValue);
        } catch (JSONException e) {}
        try {
            jsonObject.put("timestampValue", this.timestampValue);
        } catch (JSONException e) {}
        try {
            jsonObject.put("sourceValue", this.sourceValue);
        } catch (JSONException e) {}
        return jsonObject;
    }

    @Override
    public String getHostValue() {
        return hostValue;
    }

    @Override
    public String getIpValue() {
        return ipValue;
    }

    @Override
    public Long getTtlValue() {
        return ttlValue;
    }

    @Override
    public String getSourceValue() {
        return sourceValue;
    }

    @Override
    public Long getTimestampValue() {
        return timestampValue;
    }

    boolean isValid() {
        if (timestampValue == null || ipValue == null || ipValue.length() == 0) {
            return false;
        }
        int maxTTL = GlobalConfiguration.getInstance().dnsCacheMaxTTL;
        return (Utils.currentTimestamp() / 1000) < timestampValue + maxTTL;
    }

    boolean needRefresh() {
        if (timestampValue == null || ttlValue == null || ipValue == null || ipValue.length() == 0) {
            return false;
        }
        int ttl = ttlValue.intValue();
        return (Utils.currentTimestamp() / 1000)  > timestampValue + ttl;
    }
}