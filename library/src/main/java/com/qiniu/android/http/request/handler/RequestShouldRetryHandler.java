package com.qiniu.android.http.request.handler;

import com.qiniu.android.http.ResponseInfo;

import org.json.JSONObject;

/**
 * 请求重试回调
 */
public interface RequestShouldRetryHandler {

    /**
     * 请求重试回调
     *
     * @param responseInfo 上次请求的响应信息
     * @param response     上次请求的响应信息
     * @return 是否可以重试
     */
    boolean shouldRetry(ResponseInfo responseInfo, JSONObject response);
}
