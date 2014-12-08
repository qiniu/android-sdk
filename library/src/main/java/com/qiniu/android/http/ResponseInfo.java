package com.qiniu.android.http;


import java.util.Locale;

/**
 *  定义HTTP请求的日志信息和常规方法
 */
public final class ResponseInfo {
    public static final int InvalidArgument = -4;
    public static final int InvalidFile = -3;
    public static final int Cancelled = -2;
    public static final int NetworkError = -1;
    /**
     *  回复状态码
     */
    public final int statusCode;
    /**
     *  七牛日志扩展头
     */
    public final String reqId;
    /**
     *  七牛日志扩展头
     */
    public final String xlog;
    /**
     *  错误信息
     */
    public final String error;

    public ResponseInfo(int statusCode, String reqId, String xlog, String error) {
        this.statusCode = statusCode;
        this.reqId = reqId;
        this.xlog = xlog;
        this.error = error;
    }

    public static ResponseInfo cancelled() {
        return new ResponseInfo(Cancelled, "", "", "cancelled by user");
    }

    public static ResponseInfo invalidArgument(String message) {
        return new ResponseInfo(InvalidArgument, "", "", message);
    }


    public static ResponseInfo fileError(Exception e) {
        return new ResponseInfo(InvalidFile, "", "", e.getMessage());
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
        return (statusCode >= 500 && statusCode < 600 && statusCode != 579) || statusCode == 996;
    }

    public boolean needRetry() {
        return isNetworkBroken() || isServerError() || statusCode == 406 || (statusCode == 200 && error != null);
    }

    public String toString() {
        return String.format(Locale.ENGLISH, "{ResponseInfo:%s,status:%d, reqId:%s, xlog:%s,error:%s}",
                super.toString(), statusCode, reqId, xlog, error);
    }
}
