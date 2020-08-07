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


    public Long totalElapsedTime(){
        return time(startDate, endDate);
    }
    public Long totalDnsTime(){
        return time(domainLookupStartDate, domainLookupEndDate);
    }

    public Long totalConnectTime(){
        return time(connectStartDate, connectEndDate);
    }
    public Long totalSecureConnectTime(){
        return time(secureConnectionStartDate, secureConnectionEndDate);
    }

    public Long totalRequestTime(){
        return time(requestStartDate, requestEndDate);
    }
    public Long totalWaitTime(){
        return time(requestEndDate, responseStartDate);
    }
    public Long totalResponseTime(){
        return time(responseStartDate, responseEndDate);
    }

    public Long totalBytes(){
        long headerLength = 0;
        long bodyLength = 0 ;
        if (request.allHeaders != null){
            headerLength = (new JSONObject(request.allHeaders)).toString().length();
        }
        if (request.httpBody != null){
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


    private Long time(Date startDate, Date endDate){
        if (startDate != null && endDate != null){
            return (endDate.getTime() - startDate.getTime());
        } else {
            return null;
        }
    }
}
