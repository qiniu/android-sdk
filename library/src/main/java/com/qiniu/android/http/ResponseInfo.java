package com.qiniu.android.http;


import java.util.Locale;

/**
 * 定义HTTP请求的日志信息和常规方法
 */
public final class ResponseInfo {
    public static final int InvalidArgument = -4;
    public static final int InvalidFile = -3;
    public static final int Cancelled = -2;
    public static final int NetworkError = -1;

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
     * 请求消耗时间，单位秒
     */
    public final double duration;
    /**
     * 服务器域名
     */
    public final String host;
    /**
     * 服务器IP
     */
    public final String ip;

    public ResponseInfo(int statusCode, String reqId, String xlog, String xvia, String host, String ip, double duration, String error) {
        this.statusCode = statusCode;
        this.reqId = reqId;
        this.xlog = xlog;
        this.xvia = xvia;
        this.host = host;
        this.duration = duration;
        this.error = error;
        this.ip = ip;
    }

    public static ResponseInfo cancelled() {
        return new ResponseInfo(Cancelled, "", "", "", "", "", 0, "cancelled by user");
    }

    public static ResponseInfo invalidArgument(String message) {
        return new ResponseInfo(InvalidArgument, "", "", "", "", "", 0,
                message);
    }


    public static ResponseInfo fileError(Exception e) {
        return new ResponseInfo(InvalidFile, "", "", "", "", "",
                0, e.getMessage());
    }

    public boolean isCancelled() {
        return statusCode == Cancelled;
    }

    public boolean isOK() {
        return statusCode == 200 && error == null && reqId != null;
    }

    public boolean isNetworkBroken() {
        return statusCode == NetworkError;
    }

    public boolean isServerError() {
        return (statusCode >= 500 && statusCode < 600 && statusCode != 579)
                || statusCode == 996;
    }

    public boolean needSwitchServer() {
        return statusCode == NetworkError || statusCode == CannotConnectToHost
                || statusCode == TimedOut || statusCode == NetworkConnectionLost
                || (statusCode >= 500 && statusCode < 600 && statusCode != 579);
    }

    public boolean needRetry() {
        return !isCancelled() && (isNetworkBroken() || isServerError() || statusCode == 406
                || (statusCode == 200 && error != null) );
    }

    public String toString() {
        return String.format(Locale.ENGLISH, "{ResponseInfo:%s,status:%d, reqId:%s, xlog:%s, xvia:%s,  host:%s, ip:%s, duration:%f s, error:%s}",
                super.toString(), statusCode, reqId, xlog, xvia, host, ip, duration, error);
    }
}
