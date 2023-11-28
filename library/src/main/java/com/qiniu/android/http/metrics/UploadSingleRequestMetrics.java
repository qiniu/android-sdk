package com.qiniu.android.http.metrics;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.request.Request;
import com.qiniu.android.utils.Utils;

import org.json.JSONObject;

import java.util.Date;

/**
 * 单请求的指标
 */
public class UploadSingleRequestMetrics extends UploadMetrics {

    /**
     * 请求被劫持标识
     */
    public static final String RequestHijacked = "forsure";

    /**
     * 请求可能被劫持标识
     */
    public static final String RequestMaybeHijacked = "maybe";

    // 请求是否劫持【内部使用】
    private String hijacked;

    /**
     * 构造函数
     */
    public UploadSingleRequestMetrics() {
    }

    /**
     * 请求是否劫持
     *
     * @return hijacked
     */
    public String getHijacked() {
        return hijacked;
    }

    /**
     * 设置 hijacked
     *
     * @param hijacked hijacked
     */
    public void setHijacked(String hijacked) {
        this.hijacked = hijacked;
    }

    // 同步 Dns 解析源【内部使用】
    private String syncDnsSource;

    /**
     * 获取同步安全 DNS 解析的源
     *
     * @return 安全 DNS 解析的源
     */
    public String getSyncDnsSource() {
        return syncDnsSource;
    }

    /**
     * 设置同步安全 DNS 解析的源
     *
     * @param syncDnsSource 安全 DNS 解析的源
     */
    public void setSyncDnsSource(String syncDnsSource) {
        this.syncDnsSource = syncDnsSource;
    }

    // 同步 Dns 解析错误信息【内部使用】
    private String syncDnsError;

    /**
     * 获取同步 DNS 解析错误信息
     *
     * @return 同步 DNS 解析错误信息
     */
    public String getSyncDnsError() {
        return syncDnsError;
    }

    /**
     * 设置同步 DNS 解析错误信息
     *
     * @param syncDnsError 同步 DNS 解析错误信息
     */
    public void setSyncDnsError(String syncDnsError) {
        this.syncDnsError = syncDnsError;
    }

    // 网络检测信息，只有进行网络检测才会有 connectCheckMetrics 【内部使用】
    private UploadSingleRequestMetrics connectCheckMetrics;

    /**
     * 获取网络检测指标
     *
     * @return 网络检测指标
     */
    public UploadSingleRequestMetrics getConnectCheckMetrics() {
        return connectCheckMetrics;
    }

    /**
     * 设置网络检测指标
     *
     * @param connectCheckMetrics 网络检测指标
     */
    public void setConnectCheckMetrics(UploadSingleRequestMetrics connectCheckMetrics) {
        this.connectCheckMetrics = connectCheckMetrics;
    }

    // 请求对象【自定义对象，请保证对象的此属性不为空】
    private Request request;

    /**
     * 获取请求
     *
     * @return 请求
     */
    public Request getRequest() {
        return request;
    }

    /**
     * 设置请求
     *
     * @param request 请求
     */
    public void setRequest(Request request) {
        if (request != null) {
            this.request = request.copyWithoutBody();
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

    /**
     * 获取请求响应
     *
     * @return 请求响应
     */
    public ResponseInfo getResponse() {
        return response;
    }

    /**
     * 设置请求响应
     *
     * @param response 设置请求响应
     */
    public void setResponse(ResponseInfo response) {
        this.response = response;
    }

    // 实际请求使用的 httpVersion【自定义对象，请保证对象的此属性不为空】
    private String httpVersion;

    /**
     * 获取请求 HTTP 版本
     *
     * @return 请求 HTTP 版本
     */
    public String getHttpVersion() {
        return httpVersion;
    }

    /**
     * 设置请求 HTTP 版本
     *
     * @param httpVersion 请求 HTTP 版本
     */
    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    // 请求使用的 Client 名称
    private String clientName = "customized";

    /**
     * 获取 client 名
     *
     * @return client 名
     */
    public String getClientName() {
        return clientName;
    }

    /**
     * 设置 client 名
     *
     * @param clientName client 名
     */
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    // 请求使用的 Client 的版本信息
    private String clientVersion = "unknown";

    /**
     * 获取 client 版本
     *
     * @return client 版本
     */
    public String getClientVersion() {
        return clientVersion;
    }

    /**
     * 设置 client 版本
     *
     * @param clientVersion client 版本
     */
    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    // dns 解析开始时间 【自定义对象，请保证对象的此属性不为空】
    private Date domainLookupStartDate;

    /**
     * 获取 dns 解析开始时间
     *
     * @return dns 解析开始时间
     */
    public Date getDomainLookupStartDate() {
        return domainLookupStartDate;
    }

    /**
     * 配置 dns 解析开始时间
     *
     * @param domainLookupStartDate dns 解析开始时间
     */
    public void setDomainLookupStartDate(Date domainLookupStartDate) {
        this.domainLookupStartDate = domainLookupStartDate;
    }

    // dns 解析结束时间 【自定义对象，请保证对象的此属性不为空】
    private Date domainLookupEndDate;

    /**
     * 获取 dns 解析结束时间
     *
     * @return dns 解析结束时间
     */
    public Date getDomainLookupEndDate() {
        return domainLookupEndDate;
    }

    /**
     * 设置 dns 解析结束时间
     *
     * @param domainLookupEndDate dns 解析结束时间
     */
    public void setDomainLookupEndDate(Date domainLookupEndDate) {
        this.domainLookupEndDate = domainLookupEndDate;
    }

    // connect 开始时间 【自定义对象，请保证对象的此属性不为空】
    private Date connectStartDate;

    /**
     * 获取链接建立开始时间
     *
     * @return 链接建立开始时间
     */
    public Date getConnectStartDate() {
        return connectStartDate;
    }

    /**
     * 设置链接建立开始时间
     *
     * @param connectStartDate 链接建立开始时间
     */
    public void setConnectStartDate(Date connectStartDate) {
        this.connectStartDate = connectStartDate;
    }

    // connect 结束时间 【自定义对象，请保证对象的此属性不为空】
    private Date connectEndDate;

    /**
     * 获取链接建立结束时间
     *
     * @return 链接建立结束时间
     */
    public Date getConnectEndDate() {
        return connectEndDate;
    }

    /**
     * 设置链接建立结束时间
     *
     * @param connectEndDate 链接建立结束时间
     */
    public void setConnectEndDate(Date connectEndDate) {
        this.connectEndDate = connectEndDate;
    }

    // secure connect 开始时间 【自定义对象，请保证对象的此属性不为空】
    private Date secureConnectionStartDate;

    /**
     * 获取 tls 建立开始时间
     *
     * @return tls 建立开始时间
     */
    public Date getSecureConnectionStartDate() {
        return secureConnectionStartDate;
    }

    /**
     * 设置 tls 建立开始时间
     *
     * @param secureConnectionStartDate tls 建立开始时间
     */
    public void setSecureConnectionStartDate(Date secureConnectionStartDate) {
        this.secureConnectionStartDate = secureConnectionStartDate;
    }

    // secure connect 结束时间 【自定义对象，请保证对象的此属性不为空】
    private Date secureConnectionEndDate;

    /**
     * 获取 tls 建立结束时间
     *
     * @return tls 建立结束时间
     */
    public Date getSecureConnectionEndDate() {
        return secureConnectionEndDate;
    }

    /**
     * 设置 tls 建立结束时间
     *
     * @param secureConnectionEndDate tls 建立结束时间
     */
    public void setSecureConnectionEndDate(Date secureConnectionEndDate) {
        this.secureConnectionEndDate = secureConnectionEndDate;
    }

    // http 请求头发送开始时间 【自定义对象，请保证对象的此属性不为空】
    private Date requestStartDate;

    /**
     * 获取请求开始时间
     *
     * @return 请求开始时间
     */
    public Date getRequestStartDate() {
        return requestStartDate;
    }

    /**
     * 设置请求开始时间
     *
     * @param requestStartDate 请求开始时间
     */
    public void setRequestStartDate(Date requestStartDate) {
        this.requestStartDate = requestStartDate;
    }

    // http 请求体发送结束时间 【自定义对象，请保证对象的此属性不为空】
    private Date requestEndDate;

    /**
     * 获取请求结束时间
     *
     * @return 请求结束时间
     */
    public Date getRequestEndDate() {
        return requestEndDate;
    }

    /**
     * 设置请求结束时间
     *
     * @param requestEndDate 请求结束时间
     */
    public void setRequestEndDate(Date requestEndDate) {
        this.requestEndDate = requestEndDate;
    }

    // http 收到响应的时间 【自定义对象，请保证对象的此属性不为空】
    private Date responseStartDate;

    /**
     * 获取响应开始时间
     *
     * @return 响应开始时间
     */
    public Date getResponseStartDate() {
        return responseStartDate;
    }

    /**
     * 设置响应开始时间
     *
     * @param responseStartDate 响应开始时间
     */
    public void setResponseStartDate(Date responseStartDate) {
        this.responseStartDate = responseStartDate;
    }

    // http 响应的结束时间 【自定义对象，请保证对象的此属性不为空】
    private Date responseEndDate;

    /**
     * 获取响应结束时间
     *
     * @return 响应结束时间
     */
    public Date getResponseEndDate() {
        return responseEndDate;
    }

    /**
     * 设置响应结束时间
     *
     * @param responseEndDate 响应结束时间
     */
    public void setResponseEndDate(Date responseEndDate) {
        this.responseEndDate = responseEndDate;
    }

    // http 请求头大小【自定义对象，请保证对象的此属性不为空】
    private long countOfRequestHeaderBytesSent = 0;

    /**
     * 获取请求头发送的大小
     *
     * @return 请求头发送的大小
     */
    public long getCountOfRequestHeaderBytesSent() {
        return countOfRequestHeaderBytesSent;
    }

    /**
     * 设置请求头发送的大小
     *
     * @param countOfRequestHeaderBytesSent 请求头发送的大小
     */
    public void setCountOfRequestHeaderBytesSent(long countOfRequestHeaderBytesSent) {
        this.countOfRequestHeaderBytesSent = countOfRequestHeaderBytesSent;
    }

    // http 请求头体【自定义对象，请保证对象的此属性不为空】
    private long countOfRequestBodyBytesSent = 0;

    /**
     * 获取请求体发送的大小
     *
     * @return 请求体发送的大小
     */
    public long getCountOfRequestBodyBytesSent() {
        return countOfRequestBodyBytesSent;
    }

    /**
     * 设置请求体发送的大小
     *
     * @param countOfRequestBodyBytesSent 请求体发送的大小
     */
    public void setCountOfRequestBodyBytesSent(long countOfRequestBodyBytesSent) {
        this.countOfRequestBodyBytesSent = countOfRequestBodyBytesSent;
    }

    // http 响应头大小【自定义对象，请保证对象的此属性不为空】
    private long countOfResponseHeaderBytesReceived = 0;

    /**
     * 获取响应头大小
     *
     * @return 响应头大小
     */
    public long getCountOfResponseHeaderBytesReceived() {
        return countOfResponseHeaderBytesReceived;
    }

    /**
     * 设置响应头大小
     *
     * @param countOfResponseHeaderBytesReceived 响应头大小
     */
    public void setCountOfResponseHeaderBytesReceived(long countOfResponseHeaderBytesReceived) {
        this.countOfResponseHeaderBytesReceived = countOfResponseHeaderBytesReceived;
    }

    // http 响应体大小【自定义对象，请保证对象的此属性不为空】
    private long countOfResponseBodyBytesReceived = 0;

    /**
     * 获取响应体接收的大小
     *
     * @return 响应体接收的大小
     */
    public long getCountOfResponseBodyBytesReceived() {
        return countOfResponseBodyBytesReceived;
    }

    /**
     * 设置响应体接收的大小
     *
     * @param countOfResponseBodyBytesReceived 响应体接收的大小
     */
    public void setCountOfResponseBodyBytesReceived(long countOfResponseBodyBytesReceived) {
        this.countOfResponseBodyBytesReceived = countOfResponseBodyBytesReceived;
    }

    // http 请求本地 IP【自定义对象，请保证对象的此属性不为空】
    private String localAddress;

    /**
     * 获取 localAddress
     *
     * @return localAddress
     */
    public String getLocalAddress() {
        return localAddress;
    }

    /**
     * 设置 localAddress
     *
     * @param localAddress localAddress
     */
    public void setLocalAddress(String localAddress) {
        this.localAddress = localAddress;
    }

    // http 请求本地端口【自定义对象，请保证对象的此属性不为空】
    private Integer localPort;

    /**
     * 获取 local port
     *
     * @return local port
     */
    public Integer getLocalPort() {
        return localPort;
    }

    /**
     * 设置 local port
     *
     * @param localPort local port
     */
    public void setLocalPort(Integer localPort) {
        this.localPort = localPort;
    }

    // http 请求远程 IP【自定义对象，请保证对象的此属性不为空】
    private String remoteAddress;

    /**
     * 获取 server address
     *
     * @return server address
     */
    public String getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * 设置 server address
     *
     * @param remoteAddress server address
     */
    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    // http 请求远程端口【自定义对象，请保证对象的此属性不为空】
    private Integer remotePort;

    /**
     * 获取 server port
     *
     * @return server port
     */
    public Integer getRemotePort() {
        return remotePort;
    }

    /**
     * 设置 server port
     *
     * @param remotePort server port
     */
    public void setRemotePort(Integer remotePort) {
        this.remotePort = remotePort;
    }

    private long totalBytes = 0;

    /**
     * 获取 dns 耗时时间
     *
     * @return dns 耗时时间
     */
    public long totalDnsTime() {
        return time(domainLookupStartDate, domainLookupEndDate);
    }

    /**
     * 获取 建立连接耗时时间
     *
     * @return 建立连接耗时时间
     */
    public long totalConnectTime() {
        return time(connectStartDate, connectEndDate);
    }

    /**
     * 获取 tls 耗时时间
     *
     * @return tls 耗时时间
     */
    public long totalSecureConnectTime() {
        return time(secureConnectionStartDate, secureConnectionEndDate);
    }

    /**
     * 获取请求耗时时间
     *
     * @return 请求耗时时间
     */
    public long totalRequestTime() {
        return time(requestStartDate, requestEndDate);
    }

    /**
     * 获取请求等待耗时时间
     *
     * @return 请求等待耗时时间
     */
    public long totalWaitTime() {
        return time(requestEndDate, responseStartDate);
    }

    /**
     * 获取响应耗时时间
     *
     * @return 响应耗时时间
     */
    public long totalResponseTime() {
        return time(responseStartDate, responseEndDate);
    }

    /**
     * 获取请求总大小
     *
     * @return 请求总大小
     */
    public long totalBytes() {
        return totalBytes;
    }

    /**
     * 获取请求发送数据大小
     *
     * @return 请求发送数据大小
     */
    public Long bytesSend() {
        long totalBytes = totalBytes();
        long bytesSend = countOfRequestHeaderBytesSent + countOfRequestBodyBytesSent;
        if (bytesSend > totalBytes) {
            bytesSend = totalBytes;
        }
        return bytesSend;
    }

    /**
     * 获取请求接收数据大小
     *
     * @return 请求接收数据大小
     */
    public Long bytesReceived() {
        long bytesReceived = countOfResponseHeaderBytesReceived + countOfResponseBodyBytesReceived;
        if (bytesReceived < 0) {
            bytesReceived = 0;
        }
        return bytesReceived;
    }

    /**
     * 获取请求速度
     *
     * @return 请求速度
     */
    public Long perceptiveSpeed() {
        return Utils.calculateSpeed(bytesSend() + bytesReceived(), totalElapsedTime());
    }

    /**
     * 获取请求是否被劫持
     *
     * @return 请求是否被劫持
     */
    public boolean isForsureHijacked() {
        return hijacked != null && hijacked.contains(RequestHijacked);
    }

    /**
     * 获取请求是否可能被劫持
     *
     * @return 请求是否可能被劫持
     */
    public boolean isMaybeHijacked() {
        return hijacked != null && hijacked.contains(RequestMaybeHijacked);
    }

    private long time(Date startDate, Date endDate) {
        return Utils.dateDuration(startDate, endDate);
    }
}
