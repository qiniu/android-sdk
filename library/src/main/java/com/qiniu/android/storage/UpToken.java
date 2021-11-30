package com.qiniu.android.storage;

import com.qiniu.android.utils.UrlSafeBase64;

import org.json.JSONObject;

import java.util.Date;

/**
 * 内部使用的客户端 token 检查.
 */
public final class UpToken {

    public final String token;
    public final String accessKey;
    public final String bucket;

    private long deadline = -1;
    private String returnUrl = null;

    private UpToken(String returnUrl, String token, String accessKey, String bucket) {
        this.returnUrl = returnUrl;
        this.token = token;
        this.accessKey = accessKey;
        this.bucket = bucket;
    }

    public static UpToken parse(String token) {
        if (token == null) {
            return null;
        }
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
            String dtokenString = new String(dtoken);
            obj = new JSONObject(dtokenString);
        } catch (Exception e) {
            return null;
        }
        String scope = obj.optString("scope");
        String bucket = "";
        if (scope.equals("")) {
            return null;
        } else {
            String[] scopeSlice = new String[2];
            try {
                scopeSlice = scope.split(":");
            } catch (Exception e) {
            }
            if (scopeSlice.length > 0) {
                bucket = scopeSlice[0];
            }
        }

        long deadline = obj.optInt("deadline");
        if (deadline == 0) {
            return null;
        }
        UpToken upToken = new UpToken(obj.optString("returnUrl"), token, t[0], bucket);
        upToken.deadline = deadline;
        return upToken;
    }

    public static UpToken getInvalidToken() {
        UpToken token = new UpToken("", "", "", "");
        token.deadline = -1;
        return token;
    }

    public static boolean isInvalid(UpToken token) {
        return token == null || !token.isValid();
    }

    public boolean isValid() {
        return accessKey != null && !accessKey.isEmpty() && bucket != null && !bucket.isEmpty();
    }

    public String toString() {
        return token;
    }

    public boolean hasReturnUrl() {
        return !returnUrl.equals("");
    }

    public String index() {
        String index = "";
        if (accessKey != null) {
            index += accessKey;
        }
        if (bucket != null) {
            index += bucket;
        }
        return index;
    }

    public long getDeadline() {
        return deadline;
    }

    public boolean isValidForDuration(long duration) {
        return isValidBeforeTimestamp((new Date().getTime() / 1000) + duration);
    }

    public boolean isValidBeforeDate(Date date) {
        if (date == null) {
            return false;
        }
        return isValidBeforeTimestamp(date.getTime() / 1000);
    }

    private boolean isValidBeforeTimestamp(long timestamp) {
        if (deadline < 0) {
            return false;
        }
        return timestamp < deadline;
    }
}
