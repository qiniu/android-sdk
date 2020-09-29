package com.qiniu.android.http.metrics;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.request.Request;

import org.json.JSONObject;

import java.util.Date;

public class UploadSingleRequestMetrics {

    public Request request;
    public ResponseInfo response;

    public Date startDate;
    public Date endDate;

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


    public long totalElapsedTime(){
        return time(startDate, endDate);
    }
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
        }
    }

    public Long totalBytes(){
        long headerLength = 0;
        long bodyLength = 0 ;
        if (request != null && request.allHeaders != null){
            headerLength = (new JSONObject(request.allHeaders)).toString().length();
        }
        if (request != null && request.httpBody != null){
            bodyLength = request.httpBody.length;
        }
        return (headerLength + bodyLength);
    }
    public Long bytesSend(){
        long totalBytes = totalBytes().longValue();
        long bytesSend = countOfRequestHeaderBytesSent + countOfRequestBodyBytesSent;
        if (bytesSend > totalBytes){
            bytesSend = totalBytes;
        }
        return bytesSend;
    }


    private long time(Date startDate, Date endDate){
        if (startDate != null && endDate != null){
            return (endDate.getTime() - startDate.getTime());
        } else {
            return 0l;
        }
    }
}
