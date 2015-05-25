package com.qiniu.android.storage;

import com.qiniu.android.utils.UrlSafeBase64;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by bailong on 15/5/25.
 */
final class UpPolicy {
    private String returnUrl = null;
    static UpPolicy parse(String token){
        byte[] dtoken = UrlSafeBase64.decode(token);
        JSONObject obj;
        try {
            obj = new JSONObject(new String(dtoken));
        } catch (JSONException e) {
            return null;
        }
        String scope = obj.optString("scope");
        if (scope.equals("")){
            return null;
        }

        int deadline = obj.optInt("deadline");
        if (deadline == 0){
            return null;
        }
        return new UpPolicy(obj);
    }

    private UpPolicy(JSONObject obj){
        returnUrl = obj.optString("returnUrl");
    }

    private boolean hasReturnUrl(){
        return !returnUrl.equals("");
    }

}
