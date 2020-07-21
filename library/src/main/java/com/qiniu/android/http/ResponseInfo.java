package com.qiniu.android.http;


import com.qiniu.android.common.Constants;
import com.qiniu.android.http.request.Request;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Map;

/**
 * 定义HTTP请求的日志信息和常规方法
 */
public final class ResponseInfo {
    public static final int ResquestSuccess = 200;
    public static final int ZeroSizeFile = -6;
    public static final int InvalidToken = -5;
    public static final int InvalidArgument = -4;
    public static final int InvalidFile = -3;
    public static final int Cancelled = -2;
    public static final int NetworkError = -1;
    public static final int LocalIOError = -7;
    public static final int MaliciousResponseError = -8;

    public static final int Crc32NotMatch = -406;

    public static final int UnknownError = 10000;

    // <-- error code copy from ios
    public static final int TimedOut = -1001;
    public static final int UnknownHost = -1003;
    public static final int CannotConnectToHost = -1004;
    public static final int NetworkConnectionLost = -1005;
    public static final int NetworkSSLError = -1200;
    public static final int NetworkProtocolError = 100;
    public static final int NetworkSlow = -1009;
    public static final int PasrseError= -1015;

    // -->
    /**
     * 回复状态码
     */
    public final int statusCode;
    /**
     * response 信息
     */
    public String message;
    /**
     * 七牛日志扩展头
     */
    public final String reqId;
    /**
     * 七牛日志扩展头
     */
    public final String xlog;
    /**
     * cdn日志扩展头
     */
    public final String xvia;
    /**
     * 错误信息
     */
    public final String error;
    /**
     * 服务器域名
     */
    public final String host;

    /**
     * user agent id
     */
    public final String id;

    /**
     * log 时间戳
     */
    public final long timeStamp;

    /**
     * 响应头
     */
    public final Map<String, String> responseHeader;

    /**
     * 响应体，json 格式
     */
    public final JSONObject response;

    private ResponseInfo(JSONObject json,
                         Map<String, String>responseHeader,
                         int statusCode,
                         String reqId,
                         String xlog,
                         String xvia,
                         String host,
                         String error) {
        this.response = json;
        this.responseHeader = responseHeader;
        this.statusCode = statusCode;
        this.reqId = reqId != null ? reqId : "";
        this.xlog = xlog;
        this.xvia = xvia;
        this.host = host;
        this.id = UserAgent.instance().id;
        this.timeStamp = System.currentTimeMillis() / 1000;

        if (error == null && !this.isOK()) {
            String errorP = null;
            if (response != null){
                try {
                    errorP = response.getString("error");
                } catch (JSONException ignored) {}
            }
            this.error = errorP;
        } else {
            this.error = error;
        }
    }

    public static ResponseInfo zeroSize(String desc) {
        return errorInfo(ZeroSizeFile, (desc != null ? desc : "data size is 0"));
    }

    public static ResponseInfo cancelled() {
        return errorInfo(Cancelled, "cancelled by user");
    }

    public static ResponseInfo invalidArgument(String desc) {
        return errorInfo(InvalidArgument, desc);
    }

    public static ResponseInfo invalidToken(String desc) {
        return errorInfo(InvalidToken, desc);
    }

    public static ResponseInfo fileError(Exception e) {
        return errorInfo(InvalidFile, e != null ? e.getMessage() : null);
    }

    public static ResponseInfo networkError(String desc) {
        return errorInfo(NetworkError, desc);
    }

    public static ResponseInfo localIOError(String desc) {
        return errorInfo(LocalIOError, desc);
    }

    public static ResponseInfo errorInfo(int statusCode, String error) {
        ResponseInfo responseInfo = new ResponseInfo(null, null, statusCode, null, null, null, null, error);
        return responseInfo;
    }

    public static  ResponseInfo create(Request request,
                                       int responseCode,
                                       Map<String, String> responseHeader,
                                       JSONObject response,
                                       String errorMessage) {

        String host = (request != null ? request.host : null);
        String reqId = null;
        String xlog = null;
        String xvia = null;
        if (responseHeader != null) {
            reqId = responseHeader.get("x-reqid");
            xlog = responseHeader.get("x-log");
            if (responseHeader.get("x-via") != null){
                xvia = responseHeader.get("x-via");
            } else if (responseHeader.get("x-px") != null){
                xvia = responseHeader.get("x-px");
            } else if (responseHeader.get("fw-via") != null){
                xvia = responseHeader.get("fw-via");
            }
        }

        if (response != null && (reqId == null || xlog == null)){
            responseCode = MaliciousResponseError;
            errorMessage = "this is a malicious response";
            response = null;
        }

        ResponseInfo responseInfo = new ResponseInfo(response, responseHeader, responseCode, reqId, xlog, xvia, host, errorMessage);
        return responseInfo;
    }

    public static boolean isStatusCodeForBrokenNetwork(int code) {
        return code == NetworkError || code == UnknownHost
                || code == CannotConnectToHost || code == TimedOut
                || code == NetworkConnectionLost;
    }

    public boolean isCancelled() {
        return statusCode == Cancelled;
    }

    public boolean isOK() {
        return statusCode == ResquestSuccess && error == null && (hasReqId() || response != null);
    }

    public boolean couldRetry(){
        if (isCancelled()
            || (statusCode > 300 && statusCode < 400)
            || (statusCode > 400 && statusCode < 500)
            || statusCode == 501 || statusCode == 573
            || statusCode == 608 || statusCode == 612 || statusCode == 614 || statusCode == 616
            || statusCode == 619 || statusCode == 630 || statusCode == 631 || statusCode == 640
            || statusCode == 701
            ||(statusCode < 0 && statusCode > -1000)) {
            return false;
        } else {
            return true;
        }
    }

    public boolean couldRegionRetry(){
        if (!couldRetry()
            || statusCode == 400
            || statusCode == 502 || statusCode == 503 || statusCode == 504 || statusCode == 579 || statusCode == 599
            || isCancelled()) {
            return false;
        } else {
            return true;
        }
    }

    public boolean couldHostRetry(){
        if (!couldRegionRetry()
            || (statusCode == 502 || statusCode == 503 || statusCode == 571)) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isTlsError() {
        if (statusCode == NetworkSSLError){
            return true;
        } else {
            return false;
        }
    }

    public boolean isNetworkBroken() {
        return statusCode == NetworkError;
    }

    public boolean isServerError() {
        return (statusCode >= 500 && statusCode < 600 && statusCode != 579)
                || statusCode == 996;
    }

    public boolean needSwitchServer() {
        return isNetworkBroken() || isServerError();
    }

    public boolean needRetry() {
        return !isCancelled() && (needSwitchServer() || statusCode == 406 || (statusCode == 200 && error != null) || isNotQiniu());
    }

    public boolean isNotQiniu() {
        return statusCode < 500 && statusCode >= 200 && (!hasReqId() && response == null);
    }

    public String toString() {
        return String.format(Locale.ENGLISH, "{ver:%s,ResponseInfo:%s,status:%d, reqId:%s, xlog:%s, xvia:%s, host:%s, time:%d,error:%s}",
                Constants.VERSION, id, statusCode, reqId, xlog, xvia, host, timeStamp, error);
    }

    public boolean hasReqId() {
        return reqId != null;
    }

}
