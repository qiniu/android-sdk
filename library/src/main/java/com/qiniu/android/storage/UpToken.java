package com.qiniu.android.storage;

import com.qiniu.android.utils.UrlSafeBase64;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 内部使用的客户端 token 检查.
 */
public final class UpToken {
    public final String token;
    private String returnUrl = null;

    private UpToken(JSONObject obj, String token) {
        returnUrl = obj.optString("returnUrl");
        this.token = token;
    }

    public static UpToken parse(String token) {
        String[] t;
        try {
            t = token.split(":");
        } catch (Exception e) {
            return null;
        }
        if (t.length != 3) {
            return null;
        }
        byte[] dtoken = UrlSafeBase64.decode(t[2]);
        JSONObject obj;
        try {
            obj = new JSONObject(new String(dtoken));
        } catch (JSONException e) {
            return null;
        }
        String scope = obj.optString("scope");
        if (scope.equals("")) {
            return null;
        }

        int deadline = obj.optInt("deadline");
        if (deadline == 0) {
            return null;
        }
        return new UpToken(obj, token);
    }

    public String toString() {
        return token;
    }

    public boolean hasReturnUrl() {
        return !returnUrl.equals("");
    }

}
