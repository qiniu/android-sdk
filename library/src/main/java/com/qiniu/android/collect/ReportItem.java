package com.qiniu.android.collect;

import com.qiniu.android.http.ResponseInfo;

import org.json.JSONObject;

import java.util.HashMap;

/**
 * 记录信息
 */
public class ReportItem {

    private HashMap<String, Object> keyValues = new HashMap<>();

    /**
     * 构造函数
     */
    public ReportItem() {
    }

    /**
     * 添加记录内容
     *
     * @param value 记录的内容
     * @param key   记录的 key
     */
    public void setReport(Object value, String key) {
        if (key == null || value == null) {
            return;
        }
        if (value instanceof String && ((String) value).length() > 1024) {
            value = ((String) value).substring(0, 1024);
        }
        keyValues.put(key, value);
    }

    /**
     * 移除记录
     *
     * @param key 待移除记录的 key
     */
    public void removeReportValue(String key) {
        if (key == null) {
            return;
        }
        keyValues.remove(key);
    }

    /**
     * 转 json
     *
     * @return Json 数据
     */
    public String toJson() {
        String jsonString = "{}";
        if (keyValues == null || keyValues.size() == 0) {
            return jsonString;
        }
        jsonString = new JSONObject(keyValues).toString();
        return jsonString;
    }

    /**
     * request 记录类型
     */
    public static final String LogTypeRequest = "request";

    /**
     * block 记录类型
     */
    public static final String LogTypeBlock = "block";

    /**
     * quality 记录类型
     */
    public static final String LogTypeQuality = "quality";


    // 请求信息打点⽇志
    /**
     * 请求记录 key：记录类型
     */
    public static final String RequestKeyLogType = "log_type";

    /**
     * 请求记录 key：请求时间
     */
    public static final String RequestKeyUpTime = "up_time";

    /**
     * 请求记录 key：请求状态码
     */
    public static final String RequestKeyStatusCode = "status_code";

    /**
     * 请求记录 key：请求 ID
     */
    public static final String RequestKeyRequestId = "req_id";

    /**
     * 请求记录 key：请求域名
     */
    public static final String RequestKeyHost = "host";

    /**
     * 请求记录 key：请求的 HTTP 版本
     */
    public static final String RequestKeyHttpVersion = "http_version";

    /**
     * 请求记录 key：请求的服务端地址
     */
    public static final String RequestKeyRemoteIp = "remote_ip";

    /**
     * 请求记录 key：请求的端口号
     */
    public static final String RequestKeyPort = "port";

    /**
     * 请求记录 key：上传的 bucket
     */
    public static final String RequestKeyTargetBucket = "target_bucket";

    /**
     * 请求记录 key：上传保存的 key
     */
    public static final String RequestKeyTargetKey = "target_key";

    /**
     * 请求记录 key：请求总耗时
     */
    public static final String RequestKeyTotalElapsedTime = "total_elapsed_time";

    /**
     * 请求记录 key：dns 耗时
     */
    public static final String RequestKeyDnsElapsedTime = "dns_elapsed_time";

    /**
     * 请求记录 key：请求建立连接耗时
     */
    public static final String RequestKeyConnectElapsedTime = "connect_elapsed_time";

    /**
     * 请求记录 key：请求 tls 耗时
     */
    public static final String RequestKeyTLSConnectElapsedTime = "tls_connect_elapsed_time";

    /**
     * 请求记录 key：请求耗时
     */
    public static final String RequestKeyRequestElapsedTime = "request_elapsed_time";

    /**
     * 请求记录 key：等待响应耗时
     */
    public static final String RequestKeyWaitElapsedTime = "wait_elapsed_time";

    /**
     * 请求记录 key：响应耗时
     */
    public static final String RequestKeyResponseElapsedTime = "response_elapsed_time";

    /**
     * 请求记录 key：上传文件偏移量
     */
    public static final String RequestKeyFileOffset = "file_offset";

    /**
     * 请求记录 key：请求发送大小
     */
    public static final String RequestKeyBytesSent = "bytes_sent";

    /**
     * 请求记录 key：请求大小
     */
    public static final String RequestKeyBytesTotal = "bytes_total";

    /**
     * 请求记录 key：pid
     */
    public static final String RequestKeyPid = "pid";

    /**
     * 请求记录 key：tid
     */
    public static final String RequestKeyTid = "tid";

    /**
     * 请求记录 key：文件上传的目标区域
     */
    public static final String RequestKeyTargetRegionId = "target_region_id";

    /**
     * 请求记录 key：当前上传的区域
     */
    public static final String RequestKeyCurrentRegionId = "current_region_id";

    /**
     * 请求记录 key：请求错误类型
     */
    public static final String RequestKeyErrorType = "error_type";

    /**
     * 请求记录 key：请求错误信息
     */
    public static final String RequestKeyErrorDescription = "error_description";

    /**
     * 请求记录 key：上传类型
     */
    public static final String RequestKeyUpType = "up_type";

    /**
     * 请求记录 key：操作系统
     */
    public static final String RequestKeyOsName = "os_name";

    /**
     * 请求记录 key：操作系统版本
     */
    public static final String RequestKeyOsVersion = "os_version";

    /**
     * 请求记录 key：SDK 名称
     */
    public static final String RequestKeySDKName = "sdk_name";

    /**
     * 请求记录 key：SDK 版本
     */
    public static final String RequestKeySDKVersion = "sdk_version";

    /**
     * 请求记录 key：客户端时间
     */
    public static final String RequestKeyClientTime = "client_time";

    /**
     * 请求记录 key：客户端类型
     */
    public static final String RequestKeyHttpClient = "http_client";

    /**
     * 请求记录 key：客户端版本
     */
    public static final String RequestKeyHttpClientVersion = "http_client_version";

    /**
     * 请求记录 key：网络类型
     */
    public static final String RequestKeyNetworkType = "network_type";

    /**
     * 请求记录 key：信号强度
     */
    public static final String RequestKeySignalStrength = "signal_strength";

    /**
     * 请求记录 key：上次 DNS 解析源
     */
    public static final String RequestKeyPrefetchedDnsSource = "prefetched_dns_source";

    /**
     * 请求记录 key：DNS 解析源
     */
    public static final String RequestKeyDnsSource = "dns_source";

    /**
     * 请求记录 key：DNS 解析错误信息
     */
    public static final String RequestKeyDnsErrorMessage = "dns_error_message";

    /**
     * 请求记录 key：是否为劫持
     */
    public static final String RequestKeyHijacking = "hijacking";

    /**
     * 请求记录 key：DNS 解析时间
     */
    public static final String RequestKeyPrefetchedBefore = "prefetched_before";

    /**
     * 请求记录 key：DNS 解析错误信息
     */
    public static final String RequestKeyPrefetchedErrorMessage = "prefetched_error_message";

    /**
     * 请求记录 key：网络情况
     */
    public static final String RequestKeyNetworkMeasuring = "network_measuring";

    /**
     * 请求记录 key：网络上传速度
     */
    public static final String RequestKeyPerceptiveSpeed = "perceptive_speed";

    // 分块上传统计⽇志
    /**
     * 分块记录 key：日志类型
     */
    public static final String BlockKeyLogType = "log_type";

    /**
     * 分块记录 key：上传时间
     */
    public static final String BlockKeyUpTime = "up_time";

    /**
     * 分块记录 key：目标 Bucket
     */
    public static final String BlockKeyTargetBucket = "target_bucket";

    /**
     * 分块记录 key：存储的 key
     */
    public static final String BlockKeyTargetKey = "target_key";

    /**
     * 分块记录 key：目标区域 ID
     */
    public static final String BlockKeyTargetRegionId = "target_region_id";

    /**
     * 分块记录 key：当前区域 ID
     */
    public static final String BlockKeyCurrentRegionId = "current_region_id";

    /**
     * 分块记录 key：总耗时
     */
    public static final String BlockKeyTotalElapsedTime = "total_elapsed_time";

    /**
     * 分块记录 key：上传字节大小
     */
    public static final String BlockKeyBytesSent = "bytes_sent";

    /**
     * 分块记录 key：断点续传开始偏移
     */
    public static final String BlockKeyRecoveredFrom = "recovered_from";

    /**
     * 分块记录 key：上传文件大小
     */
    public static final String BlockKeyFileSize = "file_size";

    /**
     * 分块记录 key：pid
     */
    public static final String BlockKeyPid = "pid";

    /**
     * 分块记录 key：tid
     */
    public static final String BlockKeyTid = "tid";

    /**
     * 分块记录 key：上传的 api
     */
    public static final String BlockKeyUpApiVersion = "up_api_version";

    /**
     * 分块记录 key：客户端时间
     */
    public static final String BlockKeyClientTime = "client_time";

    /**
     * 分块记录 key：系统名称
     */
    public static final String BlockKeyOsName = "os_name";

    /**
     * 分块记录 key：系统版本
     */
    public static final String BlockKeyOsVersion = "os_version";

    /**
     * 分块记录 key：SDK 名
     */
    public static final String BlockKeySDKName = "sdk_name";

    /**
     * 分块记录 key：SDK 版本
     */
    public static final String BlockKeySDKVersion = "sdk_version";

    /**
     * 分块记录 key：上传速度
     */
    public static final String BlockKeyPerceptiveSpeed = "perceptive_speed";

    /**
     * 分块记录 key：是否被劫持
     */
    public static final String BlockKeyHijacking = "hijacking";


    // 上传质量统计
    /**
     * 上传质量记录 key：日志类型
     */
    public static final String QualityKeyLogType = "log_type";

    /**
     * 上传质量记录 key：上传类型
     */
    public static final String QualityKeyUpType = "up_type";

    /**
     * 上传质量记录 key：上传时间
     */
    public static final String QualityKeyUpTime = "up_time";

    /**
     * 上传质量记录 key：上传结果
     */
    public static final String QualityKeyResult = "result";

    /**
     * 上传质量记录 key：上传的目标 Bucket
     */
    public static final String QualityKeyTargetBucket = "target_bucket";

    /**
     * 上传质量记录 key：存储的 key
     */
    public static final String QualityKeyTargetKey = "target_key";

    /**
     * 上传质量记录 key：上传总耗时
     */
    public static final String QualityKeyTotalElapsedTime = "total_elapsed_time";

    /**
     * 上传质量记录 key：上传中 UC query 耗时
     */
    public static final String QualityKeyUcQueryElapsedTime = "uc_query_elapsed_time";

    /**
     * 上传质量记录 key：上传使用的请求数量
     */
    public static final String QualityKeyRequestsCount = "requests_count";

    /**
     * 上传质量记录 key：上传使用的区域数量
     */
    public static final String QualityKeyRegionsCount = "regions_count";

    /**
     * 上传质量记录 key：上传的文件大小
     */
    public static final String QualityKeyFileSize = "file_size";

    /**
     * 上传质量记录 key：上传发送的字节数
     */
    public static final String QualityKeyBytesSent = "bytes_sent";

    /**
     * 上传质量记录 key：cloud 类型
     */
    public static final String QualityKeyCloudType = "cloud_type";

    /**
     * 上传质量记录 key：上传的错误类型
     */
    public static final String QualityKeyErrorType = "error_type";

    /**
     * 上传质量记录 key：上传的错误信息
     */
    public static final String QualityKeyErrorDescription = "error_description";

    /**
     * 上传质量记录 key：操作系统名称
     */
    public static final String QualityKeyOsName = "os_name";

    /**
     * 上传质量记录 key：操作系统版本
     */
    public static final String QualityKeyOsVersion = "os_version";

    /**
     * 上传质量记录 key：SDK 名
     */
    public static final String QualityKeySDKName = "sdk_name";

    /**
     * 上传质量记录 key：SDK 版本
     */
    public static final String QualityKeySDKVersion = "sdk_version";

    /**
     * 上传质量记录 key：上传速度
     */
    public static final String QualityKeyPerceptiveSpeed = "perceptive_speed";

    /**
     * 上传质量记录 key：是否劫持
     */
    public static final String QualityKeyHijacking = "hijacking";

    /**
     * 获取请求的状态码
     *
     * @param responseInfo 请求响应
     * @return 请求的状态码
     */
    public static String requestReportStatusCode(ResponseInfo responseInfo) {
        if (responseInfo == null) {
            return null;
        } else {
            return responseInfo.statusCode + "";
        }
    }

    /**
     * 获取请求错误类型
     *
     * @param responseInfo 请求响应
     * @return 请求的错误类型
     */
    public static String requestReportErrorType(ResponseInfo responseInfo) {
        if (responseInfo == null) {
            return "unknown_error";
        }

        String errorType = null;
        if (responseInfo.statusCode > 199 && responseInfo.statusCode < 300) {

        } else if (responseInfo.statusCode > 299) {
            errorType = "response_error";
        } else if (responseInfo.statusCode == ResponseInfo.NetworkError) {
            errorType = "network_error";
        } else if (responseInfo.statusCode == ResponseInfo.TimedOut) {
            errorType = "timeout";
        } else if (responseInfo.statusCode == ResponseInfo.UnknownHost) {
            errorType = "unknown_host";
        } else if (responseInfo.statusCode == ResponseInfo.CannotConnectToHost) {
            errorType = "cannot_connect_to_host";
        } else if (responseInfo.statusCode == ResponseInfo.NetworkConnectionLost) {
            errorType = "transmission_error";
        } else if (responseInfo.statusCode == ResponseInfo.NetworkSSLError) {
            errorType = "ssl_error";
        } else if (responseInfo.statusCode == ResponseInfo.ParseError) {
            errorType = "parse_error";
        } else if (responseInfo.statusCode == ResponseInfo.MaliciousResponseError) {
            errorType = "malicious_response";
        } else if (responseInfo.statusCode == ResponseInfo.Cancelled) {
            errorType = "user_canceled";
        } else if (responseInfo.statusCode == ResponseInfo.LocalIOError) {
            errorType = "local_io_error";
        } else if (responseInfo.statusCode == ResponseInfo.NetworkProtocolError) {
            errorType = "protocol_error";
        } else if (responseInfo.statusCode == ResponseInfo.NetworkSlow) {
            errorType = "network_slow";
        } else {
            errorType = "unknown_error";
        }
        return errorType;
    }

    /**
     * 获取上传的结果
     *
     * @param responseInfo 请求响应
     * @return 上传的结果
     */
    public static String qualityResult(ResponseInfo responseInfo) {
        if (responseInfo == null) {
            return "unknown_error";
        }

        String result = null;

        if (responseInfo.statusCode > 199 && responseInfo.statusCode < 300) {
            result = "ok";
        } else if (responseInfo.statusCode > 399 &&
                (responseInfo.statusCode < 500 || responseInfo.statusCode == 573 || responseInfo.statusCode == 579 ||
                        responseInfo.statusCode == 608 || responseInfo.statusCode == 612 || responseInfo.statusCode == 614 || responseInfo.statusCode == 630 || responseInfo.statusCode == 631 ||
                        responseInfo.statusCode == 701)) {
            result = "bad_request";
        } else if (responseInfo.statusCode == ResponseInfo.ZeroSizeFile) {
            result = "zero_size_file";
        } else if (responseInfo.statusCode == ResponseInfo.InvalidFile) {
            result = "invalid_file";
        } else if (responseInfo.statusCode == ResponseInfo.InvalidToken
                || responseInfo.statusCode == ResponseInfo.InvalidArgument) {
            result = "invalid_args";
        }

        if (result == null) {
            result = requestReportErrorType(responseInfo);
        }

        return result;
    }
}

