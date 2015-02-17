package com.qiniu.android.http;

import org.json.JSONObject;

/**
 * 定义请求完成后续动作的处理接口
 */
public interface CompletionHandler {
    /**
     * 用户自定义的处理对象必须实现的接口方法
     *
     * @param info     响应的调试信息
     * @param response 响应的数据
     */
    void complete(ResponseInfo info, JSONObject response);
}
