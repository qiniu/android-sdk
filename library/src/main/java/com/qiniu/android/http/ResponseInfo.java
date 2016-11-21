package com.qiniu.android.http;


import com.qiniu.android.common.Constants;

import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.util.Locale;

/**
 * 定义HTTP请求的日志信息和常规方法
 */
public class ResponseInfo {
    public static final int ZeroSizeFile = -6;
    public static final int InvalidToken = -5;
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

    private static Constructor responseInfoConstructor;

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

    /**
     * 服务器端口
     */
    public final int port;

    /**
     * 访问路径
     */
    public final String path;

    /**
     * user agent id
     */
    public final String id;

    /**
     * log 时间戳
     */
    public final long timeStamp;

    /**
     * 已发送字节数
     */
    public final long sent;

    /**
     * hide, 内部使用
     */
    public final JSONObject response;

    {
        try {
            //Class clazz = Class.forName("com.qiniu.android.collect.ResponseInfo");
            Class clazz = Class.forName("com.qiniu.android.http.ColletResponseInfo");
            if (clazz != null) {
                Class[] paramType = {JSONObject.class, int.class, String.class, String.class, String.class, String.class,
                        String.class, String.class, int.class, double.class, long.class, String.class};
                responseInfoConstructor = clazz.getDeclaredConstructor(paramType);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

    }

    protected ResponseInfo(JSONObject json, int statusCode, String reqId, String xlog, String xvia, String host,
                        String path, String ip, int port, double duration, long sent, String error) {
        response = json;
        this.statusCode = statusCode;
        this.reqId = reqId;
        this.xlog = xlog;
        this.xvia = xvia;
        this.host = host;
        this.path = path;
        this.duration = duration;
        this.error = error;
        this.ip = ip;
        this.port = port;
        this.id = UserAgent.instance().id;
        this.timeStamp = System.currentTimeMillis() / 1000;
        this.sent = sent;
    }

    public static ResponseInfo create(JSONObject json, int statusCode, String reqId, String xlog, String xvia, String host,
                                      String path, String ip, int port, double duration, long sent, String error) {

        if(responseInfoConstructor != null) {
            try {
                System.out.println("responseInfoConstructor: sub ResponseInfo");
                return (ResponseInfo) responseInfoConstructor.
                        newInstance(json, statusCode, reqId, xlog, xvia, host, path, ip, port, duration, sent, error);
            } catch (Exception e) {
                return new ResponseInfo(json, statusCode, reqId, xlog, xvia, host, path, ip, port, duration, sent, error);
            }
        } else {
            System.out.println("responseInfoConstructor: ResponseInfo");
            return new ResponseInfo(json, statusCode, reqId, xlog, xvia, host, path, ip, port, duration, sent, error);
        }
    }

    public static ResponseInfo zeroSize() {
        return create(null, ZeroSizeFile, "", "", "", "", "", "", -1, 0, 0, "file or data size is zero");
    }

    public static ResponseInfo cancelled() {
        return create(null, Cancelled, "", "", "", "", "", "", -1, 0, 0, "cancelled by user");
    }

    public static ResponseInfo invalidArgument(String message) {
        return create(null, InvalidArgument, "", "", "", "", "", "", -1, 0, 0, message);
    }

    public static ResponseInfo invalidToken(String message) {
        return create(null, InvalidToken, "", "", "", "", "", "", -1, 0, 0, message);
    }

    public static ResponseInfo fileError(Exception e) {
        return create(null, InvalidFile, "", "", "", "", "", "", -1, 0, 0, e.getMessage());
    }

    public boolean isCancelled() {
        return statusCode == Cancelled;
    }

    public boolean isOK() {
        return statusCode == 200 && error == null && (hasReqId() || response != null);
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
        return !isCancelled() && (needSwitchServer() || statusCode == 406
                || (statusCode == 200 && error != null));
    }

    public boolean isNotQiniu() {
        return statusCode < 500 && statusCode >= 200 && (!hasReqId() && response == null);
    }

    public String toString() {
        return String.format(Locale.ENGLISH, "{ver:%s,ResponseInfo:%s,status:%d, reqId:%s, xlog:%s, xvia:%s, host:%s, path:%s, ip:%s, port:%d, duration:%f s, time:%d, sent:%d,error:%s}",
                Constants.VERSION, id, statusCode, reqId, xlog, xvia, host, path, ip, port, duration, timeStamp, sent, error);
    }

    public boolean hasReqId() {
        return reqId != null;
    }

}

// 独立到的包: com.qiniu.android.collect.ResponseInfo
// 收集上传信息的,新建一个包,和上传分开.可以单独使用上传 api,不影响上传 api 调用
// 新的包依赖上传 sdk
 class ColletResponseInfo extends ResponseInfo {

    protected ColletResponseInfo(JSONObject json, int statusCode, String reqId, String xlog, String xvia, String host, String path, String ip, int port, double duration, long sent, String error) {
        super(json, statusCode, reqId, xlog, xvia, host, path, ip, port, duration, sent, error);
        doSth();
    }

     protected void doSth() {
         System.out.println("responseInfoConstructor: do something");
     }
}
