package com.qiniu.android.http.metrics;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.request.Request;
import com.qiniu.android.utils.Utils;

import org.json.JSONObject;

import java.util.Date;

public class UploadSingleRequestMetrics extends UploadMetrics {
    public static final String RequestHijacked = "forsure";
    public static final String RequestMaybeHijacked = "maybe";

    // 请求的 httpVersion
    public String httpVersion;

    // 请求是否劫持
    public String hijacked;
    public String syncDnsSource;
    public String syncDnsError;

    // 只有进行网络检测才会有 connectCheckMetrics
    public UploadSingleRequestMetrics connectCheckMetrics;

    public Request request;
    public ResponseInfo response;

    public String clientName;
    public String clientVersion;

    public Date domainLookupStartDate;
    public Date domainLookupEndDate;

    public Date connectStartDate;
    public Date connectEndDate;

    public Date secureConnectionStartDate;
    public Date secureConnectionEndDate;

    public Date requestStartDate;
    public Date requestEndDate;

    public Date responseStartDate;
    public Date responseEndDate;

    public long countOfRequestHeaderBytesSent = 0;
    public long countOfRequestBodyBytesSent = 0;

    public long countOfResponseHeaderBytesReceived = 0;
    public long countOfResponseBodyBytesReceived = 0;

    public String localAddress;
    public Integer localPort;
    public String remoteAddress;
    public Integer remotePort;

    private long totalBytes = 0;

    public long totalDnsTime(){
        return time(domainLookupStartDate, domainLookupEndDate);
    }

    public long totalConnectTime(){
        return time(connectStartDate, connectEndDate);
    }
    public long totalSecureConnectTime(){
        return time(secureConnectionStartDate, secureConnectionEndDate);
    }

    public long totalRequestTime(){
        return time(requestStartDate, requestEndDate);
    }
    public long totalWaitTime(){
        return time(requestEndDate, responseStartDate);
    }
    public long totalResponseTime(){
        return time(responseStartDate, responseEndDate);
    }

    public void setRequest(Request request){
        if (request != null){
            this.request = new Request(request.urlString, request.httpMethod, request.allHeaders, null, request.timeout);

            long headerLength = 0;
            long bodyLength = 0 ;
            if (request.allHeaders != null){
                headerLength = (new JSONObject(request.allHeaders)).toString().length();
            }
            if (request.httpBody != null){
                bodyLength = request.httpBody.length;
            }
            totalBytes = headerLength + bodyLength;
        }
    }

    public long totalBytes(){
        return totalBytes;
    }

    public Long bytesSend(){
        long totalBytes = totalBytes();
        long bytesSend = countOfRequestHeaderBytesSent + countOfRequestBodyBytesSent;
        if (bytesSend > totalBytes){
            bytesSend = totalBytes;
        }
        return bytesSend;
    }

    public Long bytesReceived(){
        long bytesReceived = countOfResponseHeaderBytesReceived + countOfResponseBodyBytesReceived;
        if (bytesReceived < 0){
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

    private long time(Date startDate, Date endDate){
        return Utils.dateDuration(startDate, endDate);
    }
}
