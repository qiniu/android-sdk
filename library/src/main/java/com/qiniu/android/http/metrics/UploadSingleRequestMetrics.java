package com.qiniu.android.http.metrics;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.request.Request;
import com.qiniu.android.utils.Utils;

import org.json.JSONObject;

import java.util.Date;

public class UploadSingleRequestMetrics extends UploadMetrics {
    public static final String RequestHijacked = "forsure";
    public static final String RequestMaybeHijacked = "maybe";

    // 请求是否劫持【内部使用】
    private String hijacked;

    public String getHijacked() {
        return hijacked;
    }

    public void setHijacked(String hijacked) {
        this.hijacked = hijacked;
    }

    // 同步 Dns 解析源【内部使用】
    private String syncDnsSource;

    public String getSyncDnsSource() {
        return syncDnsSource;
    }

    public void setSyncDnsSource(String syncDnsSource) {
        this.syncDnsSource = syncDnsSource;
    }

    // 同步 Dns 解析错误信息【内部使用】
    private String syncDnsError;

    public String getSyncDnsError() {
        return syncDnsError;
    }

    public void setSyncDnsError(String syncDnsError) {
        this.syncDnsError = syncDnsError;
    }

    // 网络检测信息，只有进行网络检测才会有 connectCheckMetrics 【内部使用】
    private UploadSingleRequestMetrics connectCheckMetrics;

    public UploadSingleRequestMetrics getConnectCheckMetrics() {
        return connectCheckMetrics;
    }

    public void setConnectCheckMetrics(UploadSingleRequestMetrics connectCheckMetrics) {
        this.connectCheckMetrics = connectCheckMetrics;
    }

    // 请求对象【自定义对象，请保证对象的此属性不为空】
    private Request request;

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        if (request != null) {
            this.request = new Request(request.urlString, request.httpMethod, request.allHeaders, null,
                    request.connectTimeout, request.readTimeout, request.writeTimeout);

            long headerLength = 0;
            long bodyLength = 0;
            if (request.allHeaders != null) {
                headerLength = (new JSONObject(request.allHeaders)).toString().length();
            }
            if (request.httpBody != null) {
                bodyLength = request.httpBody.length;
            }
            totalBytes = headerLength + bodyLength;
        }
    }

    // 请求的响应对象【自定义对象，请保证对象的此属性不为空】
    private ResponseInfo response;

    public ResponseInfo getResponse() {
        return response;
    }

    public void setResponse(ResponseInfo response) {
        this.response = response;
    }

    // 实际请求使用的 httpVersion【自定义对象，请保证对象的此属性不为空】
    private String httpVersion;

    public String getHttpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    // 请求使用的 Client 名称
    private String clientName = "customized";

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    // 请求使用的 Client 的版本信息
    private String clientVersion = "unknown";

    public String getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    // dns 解析开始时间 【自定义对象，请保证对象的此属性不为空】
    private Date domainLookupStartDate;

    public Date getDomainLookupStartDate() {
        return domainLookupStartDate;
    }

    public void setDomainLookupStartDate(Date domainLookupStartDate) {
        this.domainLookupStartDate = domainLookupStartDate;
    }

    // dns 解析结束时间 【自定义对象，请保证对象的此属性不为空】
    private Date domainLookupEndDate;

    public Date getDomainLookupEndDate() {
        return domainLookupEndDate;
    }

    public void setDomainLookupEndDate(Date domainLookupEndDate) {
        this.domainLookupEndDate = domainLookupEndDate;
    }

    // connect 开始时间 【自定义对象，请保证对象的此属性不为空】
    private Date connectStartDate;

    public Date getConnectStartDate() {
        return connectStartDate;
    }

    public void setConnectStartDate(Date connectStartDate) {
        this.connectStartDate = connectStartDate;
    }

    // connect 结束时间 【自定义对象，请保证对象的此属性不为空】
    private Date connectEndDate;

    public Date getConnectEndDate() {
        return connectEndDate;
    }

    public void setConnectEndDate(Date connectEndDate) {
        this.connectEndDate = connectEndDate;
    }

    // secure connect 开始时间 【自定义对象，请保证对象的此属性不为空】
    private Date secureConnectionStartDate;

    public Date getSecureConnectionStartDate() {
        return secureConnectionStartDate;
    }

    public void setSecureConnectionStartDate(Date secureConnectionStartDate) {
        this.secureConnectionStartDate = secureConnectionStartDate;
    }

    // secure connect 结束时间 【自定义对象，请保证对象的此属性不为空】
    private Date secureConnectionEndDate;

    public Date getSecureConnectionEndDate() {
        return secureConnectionEndDate;
    }

    public void setSecureConnectionEndDate(Date secureConnectionEndDate) {
        this.secureConnectionEndDate = secureConnectionEndDate;
    }

    // http 请求头发送开始时间 【自定义对象，请保证对象的此属性不为空】
    private Date requestStartDate;

    public Date getRequestStartDate() {
        return requestStartDate;
    }

    public void setRequestStartDate(Date requestStartDate) {
        this.requestStartDate = requestStartDate;
    }

    // http 请求体发送结束时间 【自定义对象，请保证对象的此属性不为空】
    private Date requestEndDate;

    public Date getRequestEndDate() {
        return requestEndDate;
    }

    public void setRequestEndDate(Date requestEndDate) {
        this.requestEndDate = requestEndDate;
    }

    // http 收到响应的时间 【自定义对象，请保证对象的此属性不为空】
    private Date responseStartDate;

    public Date getResponseStartDate() {
        return responseStartDate;
    }

    public void setResponseStartDate(Date responseStartDate) {
        this.responseStartDate = responseStartDate;
    }

    // http 响应的结束时间 【自定义对象，请保证对象的此属性不为空】
    private Date responseEndDate;

    public Date getResponseEndDate() {
        return responseEndDate;
    }

    public void setResponseEndDate(Date responseEndDate) {
        this.responseEndDate = responseEndDate;
    }

    // http 请求头大小【自定义对象，请保证对象的此属性不为空】
    private long countOfRequestHeaderBytesSent = 0;

    public long getCountOfRequestHeaderBytesSent() {
        return countOfRequestHeaderBytesSent;
    }

    public void setCountOfRequestHeaderBytesSent(long countOfRequestHeaderBytesSent) {
        this.countOfRequestHeaderBytesSent = countOfRequestHeaderBytesSent;
    }

    // http 请求头体【自定义对象，请保证对象的此属性不为空】
    private long countOfRequestBodyBytesSent = 0;

    public long getCountOfRequestBodyBytesSent() {
        return countOfRequestBodyBytesSent;
    }

    public void setCountOfRequestBodyBytesSent(long countOfRequestBodyBytesSent) {
        this.countOfRequestBodyBytesSent = countOfRequestBodyBytesSent;
    }

    // http 响应头大小【自定义对象，请保证对象的此属性不为空】
    private long countOfResponseHeaderBytesReceived = 0;

    public long getCountOfResponseHeaderBytesReceived() {
        return countOfResponseHeaderBytesReceived;
    }

    public void setCountOfResponseHeaderBytesReceived(long countOfResponseHeaderBytesReceived) {
        this.countOfResponseHeaderBytesReceived = countOfResponseHeaderBytesReceived;
    }

    // http 响应体大小【自定义对象，请保证对象的此属性不为空】
    private long countOfResponseBodyBytesReceived = 0;

    public long getCountOfResponseBodyBytesReceived() {
        return countOfResponseBodyBytesReceived;
    }

    public void setCountOfResponseBodyBytesReceived(long countOfResponseBodyBytesReceived) {
        this.countOfResponseBodyBytesReceived = countOfResponseBodyBytesReceived;
    }

    // http 请求本地 IP【自定义对象，请保证对象的此属性不为空】
    private String localAddress;

    public String getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(String localAddress) {
        this.localAddress = localAddress;
    }

    // http 请求本地端口【自定义对象，请保证对象的此属性不为空】
    private Integer localPort;

    public Integer getLocalPort() {
        return localPort;
    }

    public void setLocalPort(Integer localPort) {
        this.localPort = localPort;
    }

    // http 请求远程 IP【自定义对象，请保证对象的此属性不为空】
    private String remoteAddress;

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    // http 请求远程端口【自定义对象，请保证对象的此属性不为空】
    private Integer remotePort;

    public Integer getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(Integer remotePort) {
        this.remotePort = remotePort;
    }

    private long totalBytes = 0;

    public long totalDnsTime() {
        return time(domainLookupStartDate, domainLookupEndDate);
    }

    public long totalConnectTime() {
        return time(connectStartDate, connectEndDate);
    }

    public long totalSecureConnectTime() {
        return time(secureConnectionStartDate, secureConnectionEndDate);
    }

    public long totalRequestTime() {
        return time(requestStartDate, requestEndDate);
    }

    public long totalWaitTime() {
        return time(requestEndDate, responseStartDate);
    }

    public long totalResponseTime() {
        return time(responseStartDate, responseEndDate);
    }

    public long totalBytes() {
        return totalBytes;
    }

    public Long bytesSend() {
        long totalBytes = totalBytes();
        long bytesSend = countOfRequestHeaderBytesSent + countOfRequestBodyBytesSent;
        if (bytesSend > totalBytes) {
            bytesSend = totalBytes;
        }
        return bytesSend;
    }

    public Long bytesReceived() {
        long bytesReceived = countOfResponseHeaderBytesReceived + countOfResponseBodyBytesReceived;
        if (bytesReceived < 0) {
            bytesReceived = 0;
        }
        return bytesReceived;
    }

    public Long perceptiveSpeed() {
        return Utils.calculateSpeed(bytesSend() + bytesReceived(), totalElapsedTime());
    }

    public boolean isForsureHijacked() {
        return hijacked != null && hijacked.contains(RequestHijacked);
    }

    public boolean isMaybeHijacked() {
        return hijacked != null && hijacked.contains(RequestMaybeHijacked);
    }

    private long time(Date startDate, Date endDate) {
        return Utils.dateDuration(startDate, endDate);
    }
}
