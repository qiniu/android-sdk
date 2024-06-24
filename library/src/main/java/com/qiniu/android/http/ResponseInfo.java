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

    /**
     * StatusCode >= 100 见：https://developer.qiniu.com/kodo/3928/error-responses
     */

    @Deprecated
    public static final int ResquestSuccess = 200;

    /**
     * 请求成功
     */
    public static final int RequestSuccess = 200;

    /**
     * 非预期的系统调用，使用库上传时出现的调用异常，此错误非 SDK 层业务逻辑错误。
     */
    public static final int UnexpectedSysCallError = -10;

    /**
     * 无可用于上传的 host, 所有主备 host 均已尝试
     */
    @Deprecated
    public static final int NoUsableHostError = -9;

    /**
     * 在上传时，SDK 内部业务逻辑非预期。正常情况下，此错误并会被抛掷应用层。
     * <p>
     * 此错误出现的原因一般为某个上传流程异常请求导致，实际应该抛出请求，但因为调用异常未被抛出。
     */
    public static final int SDKInteriorError = -9;

    /**
     * 劫持错误。当请求确定或可能被劫持会抛出此错误。
     */
    public static final int MaliciousResponseError = -8;

    /**
     * 本地 io 异常，可能是文件读取异常，也可能是网络 io 异常
     */
    public static final int LocalIOError = -7;

    /**
     * 空文件错误。文件不存在 或 读取文件的大小为 0
     */
    public static final int ZeroSizeFile = -6;

    /**
     * 无效 token。token 格式错误。
     */
    public static final int InvalidToken = -5;

    /**
     * 无效参数。参数设置错误。
     */
    public static final int InvalidArgument = -4;

    /**
     * 无效文件。读取文件异常。
     */
    public static final int InvalidFile = -3;

    /**
     * 用户取消
     */
    public static final int Cancelled = -2;

    /**
     * 网络错误
     */
    public static final int NetworkError = -1;

    /**
     * 上传数据 crc32 校验失败
     */
    @Deprecated
    public static final int Crc32NotMatch = -406;

    /**
     * 未知错误
     */
    public static final int UnknownError = 10000;

    // <-- error code copy from ios

    /**
     * 请求超时
     */
    public static final int TimedOut = -1001;

    /**
     * 无法解析 host
     */
    public static final int UnknownHost = -1003;

    /**
     * 请求链接 host 异常
     */
    public static final int CannotConnectToHost = -1004;

    /**
     * 网络异常断开
     */
    public static final int NetworkConnectionLost = -1005;

    /**
     * SSL 校验异常
     */
    public static final int NetworkSSLError = -1200;

    /**
     * 网络协议错误
     */
    public static final int NetworkProtocolError = 100;

    /**
     * 网络异常，没有网络，或网络环境太差。
     */
    public static final int NetworkSlow = -1009;

    /**
     * 响应解析异常
     */
    public static final int ParseError = -1015;

    /**
     * 响应解析异常
     */
    @Deprecated
    public static final int PasrseError = -1015;

    // -->
    /**
     * 回复状态码
     */
    public final int statusCode;

    /**
     * 请求使用的 http 版本信息
     */
    public final String httpVersion;

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
                         Map<String, String> responseHeader,
                         String httpVersion,
                         int statusCode,
                         String reqId,
                         String xlog,
                         String xvia,
                         String host,
                         String error) {
        this.response = json;
        this.responseHeader = responseHeader;
        this.httpVersion = httpVersion;
        this.statusCode = statusCode;
        this.reqId = reqId != null ? reqId : "";
        this.xlog = xlog;
        this.xvia = xvia;
        this.host = host;
        this.id = UserAgent.instance().id;
        this.timeStamp = System.currentTimeMillis() / 1000;

        if (error == null && !this.isOK()) {
            String errorP = null;
            if (response != null) {
                try {
                    errorP = response.getString("error");
                } catch (JSONException ignored) {
                }
            }
            this.error = errorP;
        } else {
            this.error = error;
        }
    }

    /**
     * 构造成功响应
     *
     * @return ResponseInfo
     */
    public static ResponseInfo successResponse() {
        ResponseInfo responseInfo = new ResponseInfo(null, null, null, RequestSuccess, "inter:reqid", "inter:xlog", "inter:xvia", null, null);
        return responseInfo;
    }

    /**
     * 构造文件为空响应
     *
     * @param desc 描述信息
     * @return ResponseInfo
     */
    public static ResponseInfo zeroSize(String desc) {
        return errorInfo(ZeroSizeFile, (desc != null ? desc : "data size is 0"));
    }

    /**
     * 构造取消响应
     *
     * @return ResponseInfo
     */
    public static ResponseInfo cancelled() {
        return errorInfo(Cancelled, "cancelled by user");
    }

    /**
     * 构造无效参数响应
     *
     * @param desc 描述信息
     * @return ResponseInfo
     */
    public static ResponseInfo invalidArgument(String desc) {
        return errorInfo(InvalidArgument, desc);
    }

    /**
     * 构造无效 Token 响应
     *
     * @param desc 描述信息
     * @return ResponseInfo
     */
    public static ResponseInfo invalidToken(String desc) {
        return errorInfo(InvalidToken, desc);
    }

    /**
     * 构造文件异常响应
     *
     * @param e 异常信息
     * @return ResponseInfo
     */
    public static ResponseInfo fileError(Exception e) {
        return errorInfo(InvalidFile, e != null ? e.getMessage() : null);
    }

    /**
     * 构造网络异常响应
     *
     * @param desc 描述信息
     * @return ResponseInfo
     */
    public static ResponseInfo networkError(String desc) {
        return errorInfo(NetworkError, desc);
    }

    /**
     * 构造本地调用异常响应
     *
     * @param desc 描述信息
     * @return ResponseInfo
     */
    public static ResponseInfo localIOError(String desc) {
        return errorInfo(LocalIOError, desc);
    }

    /**
     * 构造异常响应
     *
     * @param desc 描述信息
     * @return ResponseInfo
     */
    public static ResponseInfo maliciousResponseError(String desc) {
        return errorInfo(MaliciousResponseError, desc);
    }

    /**
     * 构造无上传域名异常响应
     *
     * @param desc 描述信息
     * @return ResponseInfo
     */
    @Deprecated
    public static ResponseInfo noUsableHostError(String desc) {
        return errorInfo(NoUsableHostError, desc);
    }

    /**
     * 构造 SDK 内部异常响应
     *
     * @param desc 描述信息
     * @return ResponseInfo
     */
    public static ResponseInfo sdkInteriorError(String desc) {
        return errorInfo(SDKInteriorError, desc);
    }

    /**
     * 解析错误
     *
     * @param desc 描述信息
     * @return ResponseInfo
     */
    public static ResponseInfo parseError(String desc) {
        return errorInfo(ParseError, desc);
    }

    /**
     * 构造系统调用异常响应
     *
     * @param desc 描述信息
     * @return ResponseInfo
     */
    public static ResponseInfo unexpectedSysCallError(String desc) {
        return errorInfo(UnexpectedSysCallError, desc);
    }

    /**
     * 构造异常响应
     *
     * @param statusCode 状态码
     * @param error      错误信息
     * @return ResponseInfo
     */
    public static ResponseInfo errorInfo(int statusCode, String error) {
        ResponseInfo responseInfo = new ResponseInfo(null, null, "", statusCode, null, null, null, null, error);
        return responseInfo;
    }

    /**
     * 构造响应
     *
     * @param request        请求对象
     * @param responseCode   响应码
     * @param responseHeader 响应头
     * @param response       响应信息
     * @param errorMessage   错误信息
     * @return ResponseInfo
     */
    public static ResponseInfo create(Request request,
                                      int responseCode,
                                      Map<String, String> responseHeader,
                                      JSONObject response,
                                      String errorMessage) {
        return create(request, null, responseCode, responseHeader, response, errorMessage);
    }

    /**
     * 构造响应
     *
     * @param request        请求对象
     * @param httpVersion    请求版本信息
     * @param responseCode   响应码
     * @param responseHeader 响应头
     * @param response       响应信息
     * @param errorMessage   错误信息
     * @return ResponseInfo
     */
    public static ResponseInfo create(Request request,
                                      String httpVersion,
                                      int responseCode,
                                      Map<String, String> responseHeader,
                                      JSONObject response,
                                      String errorMessage) {

        String host = (request != null ? request.getHost() : null);
        String reqId = null;
        String xlog = null;
        String xvia = null;
        if (responseHeader != null) {
            reqId = responseHeader.get("x-reqid");
            xlog = responseHeader.get("x-log");
            if (responseHeader.get("x-via") != null) {
                xvia = responseHeader.get("x-via");
            } else if (responseHeader.get("x-px") != null) {
                xvia = responseHeader.get("x-px");
            } else if (responseHeader.get("fw-via") != null) {
                xvia = responseHeader.get("fw-via");
            }
        }

        ResponseInfo responseInfo = new ResponseInfo(response, responseHeader, httpVersion, responseCode, reqId, xlog, xvia, host, errorMessage);
        return responseInfo;
    }

    /**
     * 检查是否是异常请求
     *
     * @return ResponseInfo
     */
    public ResponseInfo checkMaliciousResponse() {
        if (statusCode == 200 && (reqId == null && xlog == null)) {
            return new ResponseInfo(null, responseHeader, httpVersion, MaliciousResponseError, reqId, xlog, xvia, host, "this is a malicious response");
        } else {
            return this;
        }
    }

    /**
     * 检查请求是否因网络问题而失败
     *
     * @param code 请求响应的状态码
     * @return 是否因网络问题而失败
     */
    public static boolean isStatusCodeForBrokenNetwork(int code) {
        return code == NetworkError || code == UnknownHost
                || code == CannotConnectToHost || code == TimedOut
                || code == NetworkConnectionLost;
    }

    /**
     * 请求是否被取消
     *
     * @return 请求是否被取消
     */
    public boolean isCancelled() {
        return statusCode == Cancelled;
    }

    /**
     * 请求是否成功
     *
     * @return 请求是否成功
     */
    public boolean isOK() {
        return statusCode == RequestSuccess && error == null && (hasReqId() || xlog != null);
    }

    /**
     * 请求是否需要重试
     *
     * @return 请求是否需要重试
     */
    public boolean couldRetry() {
        if (isNotQiniu()) {
            return true;
        }

        if (isCtxExpiredError()) {
            return true;
        }

        if (isTransferAccelerationConfigureError()) {
            return true;
        }

        if (isCancelled()
                || statusCode == 100
                || (statusCode > 300 && statusCode < 400)
                || (statusCode > 400 && statusCode < 500 && statusCode != 406)
                || statusCode == 501 || statusCode == 573
                || statusCode == 608 || statusCode == 612 || statusCode == 614 || statusCode == 616
                || statusCode == 619 || statusCode == 630 || statusCode == 631 || statusCode == 640
                || (statusCode < -1 && statusCode > -1000)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 请求是否需要在区域间重试
     *
     * @return 请求是否需要重试
     */
    public boolean couldRegionRetry() {
        if (isNotQiniu()) {
            return true;
        }

        if (isTransferAccelerationConfigureError()) {
            return true;
        }

        if (isCancelled()
                || statusCode == 100
                || (statusCode > 300 && statusCode < 500 && statusCode != 406)
                || statusCode == 501 || statusCode == 573 || statusCode == 579
                || statusCode == 608 || statusCode == 612 || statusCode == 614 || statusCode == 616
                || statusCode == 619 || statusCode == 630 || statusCode == 631 || statusCode == 640
                || statusCode == 701
                || (statusCode < -1 && statusCode > -1000)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 请求是否需要在域名之间重试
     *
     * @return 请求是否需要重试
     */
    public boolean couldHostRetry() {
        if (isNotQiniu()) {
            return true;
        }

        if (isTransferAccelerationConfigureError()) {
            return false;
        }

        if (isCancelled()
                || statusCode == 100
                || (statusCode > 300 && statusCode < 500 && statusCode != 406)
                || statusCode == 501 || statusCode == 502 || statusCode == 503
                || statusCode == 571 || statusCode == 573 || statusCode == 579 || statusCode == 599
                || statusCode == 608 || statusCode == 612 || statusCode == 614 || statusCode == 616
                || statusCode == 619 || statusCode == 630 || statusCode == 631 || statusCode == 640
                || statusCode == 701
                || (statusCode < -1 && statusCode > -1000)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 请求是否发生 tls 异常
     *
     * @return 是否发生 tls 异常
     */
    public boolean isTlsError() {
        if (statusCode == NetworkSSLError) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 请求是否连接到了服务
     *
     * @return 是否连接到了服务
     */
    public boolean canConnectToHost() {
        if (statusCode > 99 || isCancelled()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 请求的 host 是否不可用
     *
     * @return host 是否不可用
     */
    public boolean isHostUnavailable() {
        // 基本不可恢复，注：会影响下次请求，范围太大可能会造成大量的timeout
        if (isTransferAccelerationConfigureError() ||
                statusCode == 502 || statusCode == 503 || statusCode == 504 || statusCode == 599) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 在断点续上传过程中，ctx 信息是否已过期
     *
     * @return ctx 信息是否已过期
     */
    public boolean isCtxExpiredError() {
        return statusCode == 701 || (statusCode == 612 && error != null && error.contains("no such uploadId"));
    }

    /**
     * 是否是加速配置错误
     *
     * @return 是否是加速配置错误
     */
    public boolean isTransferAccelerationConfigureError() {
        if (error == null) {
            return false;
        }
        return error.contains("transfer acceleration is not configured on this bucket");
    }

    /**
     * 请求是否因网络问题而失败
     *
     * @return 请求是否因网络问题而失败
     */
    public boolean isNetworkBroken() {
        return statusCode == NetworkError || statusCode == NetworkSlow;
    }

    /**
     * 请求是否是为服务端异常
     *
     * @return 请求是否是为服务端异常
     */
    public boolean isServerError() {
        return (statusCode >= 500 && statusCode < 600 && statusCode != 579)
                || statusCode == 996;
    }

    /**
     * 重试是否需要切换域名
     *
     * @return 是否需要切换域名
     */
    public boolean needSwitchServer() {
        return isNetworkBroken() || isServerError();
    }

    /**
     * 是否需要重试
     *
     * @return 是否需要重试
     */
    public boolean needRetry() {
        return !isCancelled() && (needSwitchServer() || statusCode == 406 || (statusCode == 200 && error != null) || isNotQiniu());
    }

    /**
     * 请求是否未到达七牛服务
     *
     * @return 请求是否未到达七牛服务
     */
    public boolean isNotQiniu() {
        return (statusCode == MaliciousResponseError) || (statusCode > 0 && (!hasReqId() && xlog == null));
    }

    private boolean isQiniu() {
        return !isNotQiniu();
    }

    /**
     * 获取 string 信息
     *
     * @return string 信息
     */
    public String toString() {
        return String.format(Locale.ENGLISH, "{ver:%s,ResponseInfo:%s,status:%d, reqId:%s, xlog:%s, xvia:%s, host:%s, time:%d,error:%s}",
                Constants.VERSION, id, statusCode, reqId, xlog, xvia, host, timeStamp, error);
    }

    /**
     * 是否有 reqId
     *
     * @return 是否有 reqId
     */
    public boolean hasReqId() {
        return reqId != null && reqId.length() > 0;
    }

}
