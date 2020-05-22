package com.qiniu.android.http.request.handler;

import com.qiniu.android.http.ResponseInfo;

import org.json.JSONObject;

public interface RequestShouldRetryHandler {
    public boolean shouldRetry(ResponseInfo responseInfo,
                               JSONObject response);
}
