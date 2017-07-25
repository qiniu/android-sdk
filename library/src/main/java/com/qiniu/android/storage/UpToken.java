package com.qiniu.android.storage;

import com.qiniu.android.utils.UrlSafeBase64;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 内部使用的客户端 token 检查.
 */
public final class UpToken {
    public static UpToken NULL = new UpToken("", "", "");
    public final String token;
    public final String accessKey;
    private String returnUrl = null;

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

    public String toString() {
        return token;
    }

    public boolean hasReturnUrl() {
        return !returnUrl.equals("");
    }

}
