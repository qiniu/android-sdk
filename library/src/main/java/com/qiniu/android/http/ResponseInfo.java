package com.qiniu.android.http;

import com.qiniu.android.collect.Config;
import com.qiniu.android.collect.LogHandler;
import com.qiniu.android.collect.UploadInfoCollector;
import com.qiniu.android.collect.UploadInfoElement;
import com.qiniu.android.collect.UploadInfoElementCollector;
import com.qiniu.android.common.Constants;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.utils.Json;
import com.qiniu.android.utils.StringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.util.List;
import java.util.Locale;

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
     * 请求消耗时间，单位毫秒
     */
    public final long duration;
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
    public String xClientId;
    public final UpToken upToken;

    public final long totalSize;
    public static long regions_count;
    public static long bytes_sent;
    public static long requests_count;

    /**
     * 响应体，json 格式
     */
    public final JSONObject response;

    private ResponseInfo(JSONObject json, int statusCode, String xClientId, String reqId, String xlog, String xvia, String host,
                         String path, String ip, int port, long duration, long sent, String error, UpToken upToken, long totalSize) {
        response = json;
        this.statusCode = statusCode;
        this.xClientId = xClientId;
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
        this.upToken = upToken;
        this.totalSize = totalSize;
    }

    public static ResponseInfo create(final LogHandler logHandler, final JSONObject json, final int statusCode, final String reqId,
                                      final String xlog, final String xvia, final String host,
                                      final String path, final String oIp, final int port, final long duration,
                                      final long sent, final String error, final UpToken upToken, final long totalSize) {
        bytes_sent = bytes_sent + sent;
        requests_count += 1;
        String _ip = (oIp + "").split(":")[0];
        final String ip = _ip.substring(Math.max(0, _ip.indexOf("/") + 1));
        String xClientId = UploadInfoElement.x_log_client_id;
        ResponseInfo res = new ResponseInfo(json, statusCode, xClientId, reqId, xlog, xvia, host, path, ip,
                port, duration, sent, error, upToken, totalSize);
        if (Config.isRecord) {
            final String _timeStamp = res.timeStamp + "";
            UploadInfoCollector.handleHttp(upToken,
                    // 延迟序列化.如果判断不记录,则不执行序列化
                    new UploadInfoCollector.RecordMsg() {
                        @Override
                        public String toRecordMsg() {
                            logHandler.send("pid", (long) android.os.Process.myPid());
                            //未经过网络请求，本地校验错误时返回的logHandler == null
                            if (logHandler == null) {
                                return "";
                            }
                            logHandler.send("status_code", statusCode);
                            logHandler.send("req_id", reqId);
                            logHandler.send("host", host);
                            logHandler.send("remote_ip", ip);
                            logHandler.send("port", port);
                            if (upToken.token != "" && upToken.token != null) {
                                logHandler.send("target_bucket", StringUtils.getScope(upToken.token));
                            }
                            logHandler.send("bytes_sent", (long) sent);
                            List<InetAddress> resolveResults = DnsPrefetcher.getDnsPrefetcher().getInetAddressByHost(host);
                            if (resolveResults != null) {
                                logHandler.send("prefetched_ip_count", (long) resolveResults.size());
                            }
                            if (error != null) {
                                logHandler.send("error_type", UploadInfoElement.errorType(statusCode));
                                logHandler.send("error_description", error);
                            }

                            UploadInfoElement.ReqInfo reqInfoQuery = (UploadInfoElement.ReqInfo) logHandler.getUploadInfo();
                            UploadInfoElementCollector.setReqCommonElements(reqInfoQuery);
                            String req = Json.object2Json(reqInfoQuery);
                            return req;
                        }
                    });
        }
        return res;
    }

    // 通过path ，解析出是 form, mkblk, bput, mkfile
    private static String getUpType(String path) {
        if (path == null || !path.startsWith("/")) {
            return "";
        }
        if ("/".equals(path)) {
            return "form";
        }
        int l = path.indexOf('/', 1);
        if (l < 1) {
            return "";
        }
        String m = path.substring(1, l);
        switch (m) {
            case "mkblk":
                return "mkblk";
            case "bput":
                return "bput";
            case "mkfile":
                return "mkfile";
            case "put":
                return "put";
            default:
                return "";
        }
    }

    public static ResponseInfo errorInfo(ResponseInfo old, int statusCode, String error) {
        ResponseInfo _new = new ResponseInfo(old.response, statusCode, UploadInfoElement.x_log_client_id, old.reqId, old.xlog, old.xvia, old.host,
                old.path, old.ip, old.port, old.duration, old.sent, error, old.upToken, old.totalSize);
        return _new;
    }

    public static ResponseInfo zeroSize(final UpToken upToken) {
        return create(null, null, ZeroSizeFile, "", "", "", "", "", "", 80, 0, 0, "file or data size is zero", upToken, 0);
    }

    public static ResponseInfo cancelled(final UpToken upToken) {
        return create(null, null, Cancelled, "", "", "", "", "", "", 80, -1, -1, "cancelled by user", upToken, 0);
    }

    public static ResponseInfo invalidArgument(String message, final UpToken upToken) {
        return create(null, null, InvalidArgument, "", "", "", "", "", "", 80, 0, 0, message, upToken, 0);
    }

    public static ResponseInfo invalidToken(String message) {
        return create(null, null, InvalidToken, "", "", "", "", "", "", 80, 0, 0, message, null, 0);
    }

    public static ResponseInfo fileError(Exception e, final UpToken upToken) {
        return create(null, null, InvalidFile, "", "", "", "", "", "", 80, 0, 0, e.getMessage(), upToken, 0);
    }

    public static ResponseInfo networkError(int code, UpToken upToken) {
        return create(null, null, code, "", "", "", "", "", "", 80, 0, 0, "Network error during preQuery, Please check your network or " +
                "use http try again", upToken, 0);
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
        return !isCancelled() && (
                needSwitchServer() || statusCode == 406 ||
                        (statusCode == 200 && error != null) || (isNotQiniu() && !upToken.hasReturnUrl())
        );
    }

    public boolean isNotQiniu() {
        return statusCode < 500 && statusCode >= 200 && (!hasReqId() && response == null);
    }

    public String toString() {
        return String.format(Locale.ENGLISH, "{ver:%s,ResponseInfo:%s,status:%d, xClientId:%s, reqId:%s, xlog:%s, xvia:%s, host:%s, path:%s, ip:%s, port:%d, duration:%d s, time:%d, sent:%d,error:%s}",
                Constants.VERSION, id, statusCode, xClientId, reqId, xlog, xvia, host, path, ip, port, duration, timeStamp, sent, error);
    }

    public boolean hasReqId() {
        return reqId != null;
    }

}
