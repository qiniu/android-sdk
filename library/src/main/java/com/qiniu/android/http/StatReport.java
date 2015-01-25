package com.qiniu.android.http;

import org.apache.http.message.BasicHeader;

/**
 * Report the client status to Server.
 */
public class StatReport {
    /**
     * Convert response info to Server Header
     * @param info response info
     * @return header
     */
    public static BasicHeader xstat(ResponseInfo info){
        String reqId = info.reqId != null ? info.reqId : "";
        String cdnId = info.xvia != null ? info.xvia : "";

        return new BasicHeader("X-Stat", String.format("v1;%d;%s;%s%s", info.statusCode, reqId, cdnId, info.ip));
    }
}
