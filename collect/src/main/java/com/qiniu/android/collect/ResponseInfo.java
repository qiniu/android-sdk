package com.qiniu.android.collect;

import org.json.JSONObject;

/**
 * Created by Simon on 11/21/16.
 */
public class ResponseInfo extends com.qiniu.android.http.ResponseInfo {

    protected ResponseInfo(JSONObject json, int statusCode, String reqId, String xlog, String xvia, String host, String path, String ip, int port, double duration, long sent, String error) {
        super(json, statusCode, reqId, xlog, xvia, host, path, ip, port, duration, sent, error);
        record();
    }

    protected  String getRecordMsg() {
        //TODO 转换为需要的格式, error 信息可能有换行符,需要 base64
        return toString();
    }

    private void record() {
        CollectManager handler = null;
        handler.handle(this.getRecordMsg());
    }




}
