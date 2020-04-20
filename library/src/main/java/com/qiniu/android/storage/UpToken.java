package com.qiniu.android.storage;

import com.qiniu.android.collect.LogHandler;
import com.qiniu.android.common.FixedZone;
import com.qiniu.android.utils.UrlSafeBase64;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * 内部使用的客户端 token 检查.
 */
public final class UpToken {
    public static UpToken NULL = new UpToken("", "", "");
    public final String token;
    public final String accessKey;
    private String returnUrl = null;
    private static boolean customRegion = false;

    private UpToken(String returnUrl, String token, String accessKey) {
        this.returnUrl = returnUrl;
        this.token = token;
        this.accessKey = accessKey;
    }

    public static UpToken parse(String token) {
        String[] t;
        try {
            t = token.split(":");
        } catch (Exception e) {
            return NULL;
        }
        if (t.length != 3) {
            return NULL;
        }
        byte[] dtoken = UrlSafeBase64.decode(t[2]);
        JSONObject obj;
        try {
            obj = new JSONObject(new String(dtoken));
        } catch (JSONException e) {
            return NULL;
        }
        String scope = obj.optString("scope");
        if (scope.equals("")) {
            return NULL;
        }

        int deadline = obj.optInt("deadline");
        if (deadline == 0) {
            return NULL;
        }
        return new UpToken(obj.optString("returnUrl"), token, t[0]);
    }

    public static boolean isInvalid(UpToken token) {
        return token == null || token == NULL;
    }

    public String toString() {
        return token;
    }

    public boolean hasReturnUrl() {
        return !returnUrl.equals("");
    }

    public static void setCurrent_region_id(LogHandler logHandler, String upHost) {
        if (upHost == null || upHost == "") {
            return;
        }
        String[] hosts = upHost.split("//");
        String host = "";
        if (hosts.length > 1) {
            host = hosts[1];
        } else {
            host = hosts[0];
        }
        if (Arrays.asList(FixedZone.arrayzone0).contains(host)) {
            if (logHandler != null)
                logHandler.send("current_region_id", "z0");
        } else if (Arrays.asList(FixedZone.arrayzone1).contains(host)) {
            if (logHandler != null)
                logHandler.send("current_region_id", "z1");
        } else if (Arrays.asList(FixedZone.arrayzone2).contains(host)) {
            if (logHandler != null)
                logHandler.send("current_region_id", "z2");
        } else if (Arrays.asList(FixedZone.arrayzoneNa0).contains(host)) {
            if (logHandler != null)
                logHandler.send("current_region_id", "na0");
        } else if (Arrays.asList(FixedZone.arrayZoneAs0).contains(host)) {
            if (logHandler != null)
                logHandler.send("current_region_id", "as0");
        } else {
            customRegion = true;
        }
    }

    public static boolean isCustomRegion() {
        return customRegion;
    }

    public static void setCustomRegion(boolean customRegion) {
        UpToken.customRegion = customRegion;
    }
}
