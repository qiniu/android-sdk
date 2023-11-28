package com.qiniu.android.storage;

import com.qiniu.android.utils.UrlSafeBase64;

import org.json.JSONObject;

import java.util.Date;

/**
 * 内部使用的客户端 token 检查.
 */
public final class UpToken {

    /**
     * token
     */
    public final String token;

    /**
     * accessKey
     */
    public final String accessKey;

    /**
     * bucket
     */
    public final String bucket;

    private long deadline = -1;
    private String returnUrl = null;

    private UpToken(String returnUrl, String token, String accessKey, String bucket) {
        this.returnUrl = returnUrl;
        this.token = token;
        this.accessKey = accessKey;
        this.bucket = bucket;
    }

    /**
     * 构造函数
     *
     * @param token token
     * @return UpToken
     */
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

    /**
     * 获取无效 Token
     *
     * @return UpToken
     */
    public static UpToken getInvalidToken() {
        UpToken token = new UpToken("", "", "", "");
        token.deadline = -1;
        return token;
    }

    /**
     * 判断 Token 是否有效
     *
     * @param token Token
     * @return 是否有效
     */
    public static boolean isInvalid(UpToken token) {
        return token == null || !token.isValid();
    }

    /**
     * 判断当前 Token 是否有效
     *
     * @return 是否有效
     */
    public boolean isValid() {
        return accessKey != null && !accessKey.isEmpty() && bucket != null && !bucket.isEmpty();
    }

    /**
     * 获取 Token 字符串
     *
     * @return Token 字符串
     */
    public String toString() {
        return token;
    }

    /**
     * 判断 Token 中是否有 return url
     *
     * @return Token 中是否有 return url
     */
    public boolean hasReturnUrl() {
        return !returnUrl.equals("");
    }

    /**
     * Token 标识
     *
     * @return Token 标识
     */
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

    /**
     * 获取 Token 有效期
     *
     * @return Token 有效期
     */
    public long getDeadline() {
        return deadline;
    }

    /**
     * 在未来时间段中，Token 是否有效
     *
     * @param duration 未来时间段
     * @return Token 是否有效
     */
    public boolean isValidForDuration(long duration) {
        return isValidBeforeTimestamp((new Date().getTime() / 1000) + duration);
    }

    /**
     * 在时间之前，Token 是否有效
     *
     * @param date 时间
     * @return 是否有效
     */
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
