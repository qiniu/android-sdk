package com.qiniu.android.http;

import org.apache.http.message.BasicHeader;
import org.apache.http.Header;

/**
 * Report the client status to Server.
 */
public final class StatReport implements IReport{
    private ResponseInfo previousErrorInfo = null;
    private ResponseInfo previousSpeedInfo = null;

    /**
     * Convert response info to Server Header
     *
     * @param headers origin headers
     * @return header
     */
    public synchronized Header[] appendStatHeaders(Header[] headers) {
        if (previousErrorInfo == null && previousSpeedInfo == null) {
            return headers;
        }
        int count = 1;
        if (previousErrorInfo != null && previousSpeedInfo != null) {
            count = 2;
        }

        Header[] h;
        h = new Header[headers.length + count];
        System.arraycopy(headers, 0, h, 0, headers.length);

        if (previousErrorInfo != null) {
            ResponseInfo einfo = previousErrorInfo;
            String reqId = einfo.reqId != null ? einfo.reqId : "";
            String cdnId = einfo.xvia != null ? einfo.xvia : "";
            h[headers.length] = new BasicHeader("X-Estat",
                    String.format("e1;%d;%s;%s;%s;%f",
                            einfo.statusCode, reqId, cdnId, einfo.ip, einfo.duration));
            previousErrorInfo = null; //clear the error

        }
        if (previousSpeedInfo != null) {
            h[headers.length + count - 1] = new BasicHeader("X-Stat",
                    String.format("v1;%s;%f;%s;%s", previousSpeedInfo.reqId,
                            previousSpeedInfo.duration, previousSpeedInfo.xvia,
                            previousSpeedInfo.ip));
        }
        return h;
    }

    public synchronized void updateErrorInfo(ResponseInfo info) {
        previousErrorInfo = info;

    }

    public synchronized void updateSpeedInfo(ResponseInfo info) {
        previousSpeedInfo = info;
    }
}
