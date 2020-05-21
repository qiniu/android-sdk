package com.qiniu.android.http;


import com.qiniu.android.collect.Config;
import com.qiniu.android.collect.UploadInfoCollector;
import com.qiniu.android.common.Constants;
import com.qiniu.android.http.newHttp.Request;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.utils.StringUtils;

import org.json.JSONObject;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 定义HTTP请求的日志信息和常规方法
 */
public final class ResponseInfo {
    public static final int ZeroSizeFile = -6;
    public static final int InvalidToken = -5;
    public static final int InvalidArgument = -4;
    public static final int InvalidFile = -3;
    public static final int Cancelled = -2;
    public static final int NetworkError = -1;

    public static final int Crc32NotMatch = -406;

    public static final int UnknownError = 0;

    // <-- error code copy from ios
    public static final int TimedOut = -1001;
    public static final int UnknownHost = -1003;
    public static final int CannotConnectToHost = -1004;
    public static final int NetworkConnectionLost = -1005;

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
     * 响应体，json 格式
     */
    public final JSONObject response;

    private ResponseInfo(JSONObject json,
                         int statusCode,
                         String reqId,
                         String xlog,
                         String xvia,
                         String host,
                         String error) {
        this.response = json;
        this.statusCode = statusCode;
        this.reqId = reqId;
        this.xlog = xlog;
        this.xvia = xvia;
        this.host = host;
        this.error = error;
        this.id = UserAgent.instance().id;
        this.timeStamp = System.currentTimeMillis() / 1000;
    }

    public static ResponseInfo zeroSize(String path) {
        String desc = null;
        if (path == null){
            desc = "data size is 0";
        } else {
            desc = String.format("file %s size is 0", path);
        }
        return errorInfo(ZeroSizeFile, desc);
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
        return errorInfo(InvalidFile, e.getMessage());
    }

    public static ResponseInfo networkError(String desc) {
        return errorInfo(NetworkError, desc);
    }

    public static ResponseInfo errorInfo(int statusCode, String error) {
        ResponseInfo responseInfo = new ResponseInfo(null, statusCode, null, null, null, null, error);
        return responseInfo;
    }

    public static  ResponseInfo create(Request request,
                                       int responseCode,
                                       Map<String, String> responseHeader,
                                       JSONObject response,
                                       String errorMessage) {

        String host = (request != null ? request.host() : null);
        String reqId = null;
        String xlog = null;
        String xvia = null;
        if (responseHeader != null) {
            responseHeader.get("X-Reqid");
            responseHeader.get("X-Log");
            if (responseHeader.get("X-Via") != null){
                xvia = responseHeader.get("X-Via");
            } else if (responseHeader.get("X-Px") != null){
                xvia = responseHeader.get("X-Px");
            } else if (responseHeader.get("Fw-Via") != null){
                xvia = responseHeader.get("Fw-Via");
            }
        }
        ResponseInfo responseInfo = new ResponseInfo(response, responseCode, reqId, xlog, xvia, host, errorMessage);
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
        return statusCode == 200 && error == null && (hasReqId() || response != null);
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
        if (couldRetry() == false
            || statusCode == 400
            || statusCode == 502 || statusCode == 503 || statusCode == 504 || statusCode == 579 || statusCode == 599
            || isCancelled()) {
            return false;
        } else {
            return true;
        }
    }

    public boolean couldHostRetry(){
        if (couldRegionRetry() == false
            || (statusCode == 502 || statusCode == 503 || statusCode == 571)) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isTlsError() {
        return false;
    }

    public boolean isNetworkBroken() {
        return statusCode == NetworkError || statusCode == UnknownHost
                || statusCode == CannotConnectToHost || statusCode == TimedOut
                || statusCode == NetworkConnectionLost;
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
