package com.qiniu.android.storage;

import com.qiniu.android.http.ResponseInfo;

import org.json.JSONObject;

public interface UpCompletionHandler {
    void complete(String key, ResponseInfo info, JSONObject response);
}
