package com.qiniu.android.collect;

import com.qiniu.android.http.ResponseInfo;

import org.json.JSONObject;

import java.util.HashMap;

public class ReportItem {

    private HashMap<String, Object> keyValues = new HashMap<>();

    public ReportItem(){}

    public void setReport(Object value, String key){
        if (key == null || value == null){
            return;
        }
        keyValues.put(key, value);
    }

    public void removeReportValue(String key){
        if (key == null){
            return;
        }
        keyValues.remove(key);
    }

    public String toJson(){
        String jsonString = "{}";
        if (keyValues == null || keyValues.size() == 0){
            return jsonString;
        }
        jsonString = new JSONObject(keyValues).toString();
        return jsonString;
    }

    // 日志类型
    public static final String LogTypeRequest = "request";
    public static final String LogTypeBlock = "block";
    public static final String LogTypeQuality = "quality";


    // 请求信息打点⽇志
    public static final String RequestKeyLogType = "log_type";
    public static final String RequestKeyUpTime = "up_time";
    public static final String RequestKeyStatusCode = "status_code";
    public static final String RequestKeyRequestId = "req_id";
    public static final String RequestKeyHost = "host";
    public static final String RequestKeyRemoteIp = "remote_ip";
    public static final String RequestKeyPort = "port";
    public static final String RequestKeyTargetBucket = "target_bucket";
    public static final String RequestKeyTargetKey = "target_key";
    public static final String RequestKeyTotalElapsedTime = "total_elapsed_time";
    public static final String RequestKeyDnsElapsedTime = "dns_elapsed_time";
    public static final String RequestKeyConnectElapsedTime = "connect_elapsed_time";
    public static final String RequestKeyTLSConnectElapsedTime = "tls_connect_elapsed_time";
    public static final String RequestKeyRequestElapsedTime = "request_elapsed_time";
    public static final String RequestKeyWaitElapsedTime = "wait_elapsed_time";
    public static final String RequestKeyResponseElapsedTime = "response_elapsed_time";
    public static final String RequestKeyFileOffset = "file_offset";
    public static final String RequestKeyBytesSent = "bytes_sent";
    public static final String RequestKeyBytesTotal = "bytes_total";
    public static final String RequestKeyPid = "pid";
    public static final String RequestKeyTid = "tid";
    public static final String RequestKeyTargetRegionId = "target_region_id";
    public static final String RequestKeyCurrentRegionId = "current_region_id";
    public static final String RequestKeyErrorType = "error_type";
    public static final String RequestKeyErrorDescription = "error_description";
    public static final String RequestKeyUpType = "up_type";
    public static final String RequestKeyOsName = "os_name";
    public static final String RequestKeyOsVersion = "os_version";
    public static final String RequestKeySDKName = "sdk_name";
    public static final String RequestKeySDKVersion = "sdk_version";
    public static final String RequestKeyClientTime = "client_time";
    public static final String RequestKeyNetworkType = "network_type";
    public static final String RequestKeySignalStrength = "signal_strength";
    public static final String RequestKeyPrefetchedDnsSource = "prefetched_dns_source";
    public static final String RequestKeyPrefetchedBefore = "prefetched_before";
    public static final String RequestKeyPrefetchedErrorMessage = "prefetched_error_message";

    // 分块上传统计⽇志
    public static final String BlockKeyLogType = "log_type";
    public static final String BlockKeyUpTime = "up_time";
    public static final String BlockKeyTargetRegionId = "target_region_id";
    public static final String BlockKeyCurrentRegionId = "current_region_id";
    public static final String BlockKeyTotalElapsedTime = "total_elapsed_time";
    public static final String BlockKeyBytesSent = "bytes_sent";
    public static final String BlockKeyRecoveredFrom = "recovered_from";
    public static final String BlockKeyFileSize = "file_size";
    public static final String BlockKeyPid = "pid";
    public static final String BlockKeyTid = "tid";
    public static final String BlockKeyUpApiVersion = "up_api_version";
    public static final String BlockKeyClientTime = "client_time";


    // 上传质量统计
    public static final String QualityKeyLogType = "log_type";
    public static final String QualityKeyUpTime = "up_time";
    public static final String QualityKeyResult = "result";
    public static final String QualityKeyTotalElapsedTime = "total_elapsed_time";
    public static final String QualityKeyRequestsCount = "requests_count";
    public static final String QualityKeyRegionsCount = "regions_count";
    public static final String QualityKeyBytesSent = "bytes_sent";
    public static final String QualityKeyCloudType = "cloud_type";


    public static String requestReportStatusCode(ResponseInfo responseInfo){
        if (responseInfo.statusCode > -10 && responseInfo.statusCode < 0) {
            return String.format("%d", responseInfo.statusCode);
        } else {
            return null;
        }
    }

    public static String requestReportErrorType(ResponseInfo responseInfo){
        String errorType = null;
        if (responseInfo.statusCode > 199 && responseInfo.statusCode < 300) {

        } else if (responseInfo.statusCode > 299 && responseInfo.statusCode < 600){
            errorType = "response_error";
        } else if (responseInfo.statusCode == ResponseInfo.NetworkError){
            errorType = "network_error";
        } else if (responseInfo.statusCode == ResponseInfo.TimedOut){
            errorType = "timeout";
        } else if (responseInfo.statusCode == ResponseInfo.CannotConnectToHost){
            errorType = "cannot_connect_to_host";
        } else if (responseInfo.statusCode == ResponseInfo.NetworkConnectionLost){
            errorType = "transmission_error";
        } else if (responseInfo.statusCode == ResponseInfo.NetworkSSLError){
            errorType = "ssl_error";
        } else if (responseInfo.statusCode == ResponseInfo.PasrseError){
            errorType = "parse_error";
        } else if (responseInfo.statusCode == ResponseInfo.MaliciousResponseError){
            errorType = "malicious_response";
        } else if (responseInfo.statusCode == ResponseInfo.Cancelled){
            errorType = "user_canceled";
        } else if (responseInfo.statusCode == ResponseInfo.LocalIOError){
            errorType = "local_io_error";
        } else if (responseInfo.statusCode == ResponseInfo.NetworkProtocolError){
            errorType = "protocol_error";
        } else if (responseInfo.statusCode == ResponseInfo.NetworkSlow){
            errorType = "network_slow";
        } else {
            errorType = "unknown_error";
        }
        return errorType;
    }

    public static String qualityResult(ResponseInfo responseInfo){

        String result = null;

        if (responseInfo.statusCode > 199 && responseInfo.statusCode < 300) {
            result = "ok";
        } else if (responseInfo.statusCode > 399 && responseInfo.statusCode < 500) {
            result = "bad_request";
        } else if (responseInfo.statusCode == ResponseInfo.ZeroSizeFile){
            result = "zero_size_file";
        } else if (responseInfo.statusCode == ResponseInfo.InvalidFile){
            result = "invalid_file";
        } else if (responseInfo.statusCode == ResponseInfo.InvalidToken
                || responseInfo.statusCode == ResponseInfo.InvalidArgument){
            result = "invalid_args";
        }

        if (result == null){
            result = requestReportErrorType(responseInfo);
        }

        return result;
    }
}

