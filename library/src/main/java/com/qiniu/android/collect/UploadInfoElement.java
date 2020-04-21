package com.qiniu.android.collect;

import android.util.Log;

import java.util.concurrent.atomic.AtomicLong;

public class UploadInfoElement {

    /**
     * 本次上传的header,存活于app启动后的整个生命周期
     */
    public static String x_log_client_id = "";

    public static class ReqInfo {
        public String log_type;
        private int status_code;
        private String req_id;
        private String host;
        private String remote_ip;
        private int port;
        private String target_bucket;
        private String target_key;
        private long total_elapsed_time;
        private long dns_elapsed_time;
        private long connect_elapsed_time;
        private long request_elapsed_time;
        private long tls_connect_elapsed_time;
        private long wait_elapsed_time;
        private long response_elapsed_time;
        private long file_offset;
        /**
         * query的bytes_sent和bytes_total=0
         */
        private long bytes_sent;
        private long bytes_total;
        private long pid;
        private long tid;
        //在AutoZone或者dnsprefetch之后，根据host得到区域，如果自定义域名且本次没有dns预取，字段将为空
        private String target_region_id;
        private String error_type;
        private String error_description;
        private String up_type;
        private String os_name;
        private String os_version;
        private String sdk_name;
        private String sdk_version;
        private long up_time;
        private String network_type;
        private long signal_strength;
        private long prefetched_ip_count;

        ReqInfo() {
            this.log_type = "request";
            this.os_name = "android";
        }

        public void setLog_type(String log_type) {
            this.log_type = log_type;
        }

        public String getLog_type() {
            return log_type;
        }

        public int getStatus_code() {
            return status_code;
        }

        public String getReq_id() {
            return req_id;
        }

        public String getHost() {
            return host;
        }

        public String getRemote_ip() {
            return remote_ip;
        }

        public int getPort() {
            return port;
        }

        public String getTarget_bucket() {
            return target_bucket;
        }

        public String getTarget_key() {
            return target_key;
        }

        public long getTotal_elapsed_time() {
            return total_elapsed_time;
        }

        public long getDns_elapsed_time() {
            return dns_elapsed_time;
        }

        public long getConnect_elapsed_time() {
            return connect_elapsed_time;
        }

        public long getTls_connect_elapsed_time() {
            return tls_connect_elapsed_time;
        }

        public long getWait_elapsed_time() {
            return wait_elapsed_time;
        }

        public long getResponse_elapsed_time() {
            return response_elapsed_time;
        }

        public long getFile_offset() {
            return file_offset;
        }

        public long getBytes_sent() {
            return bytes_sent;
        }

        public long getBytes_total() {
            return bytes_total;
        }

        public long getPid() {
            return pid;
        }

        public long getTid() {
            return tid;
        }

        public String getTarget_region_id() {
            return target_region_id;
        }

        public String getError_type() {
            return error_type;
        }

        public String getError_description() {
            return error_description;
        }

        public String getUp_type() {
            return up_type;
        }

        public String getOs_name() {
            return os_name;
        }

        public String getOs_version() {
            return os_version;
        }

        public String getSdk_name() {
            return sdk_name;
        }

        public String getSdk_version() {
            return sdk_version;
        }

        public long getUp_time() {
            return up_time;
        }

        public String getNetwork_type() {
            return network_type;
        }

        public long getSignal_strength() {
            return signal_strength;
        }

        public long getPrefetched_ip_count() {
            return prefetched_ip_count;
        }

        public void setUp_type(String up_type) {
            this.up_type = up_type;
        }

        public void setStatus_code(int status_code) {
            this.status_code = status_code;
        }

        public void setReq_id(String req_id) {
            this.req_id = req_id;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public void setRemote_ip(String remote_ip) {
            this.remote_ip = remote_ip;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public void setTarget_bucket(String target_bucket) {
            this.target_bucket = target_bucket;
        }

        public void setTarget_key(String target_key) {
            this.target_key = target_key;
        }

        public void setTotal_elapsed_time(long total_elapsed_time) {
            this.total_elapsed_time = total_elapsed_time;
        }

        public void setDns_elapsed_time(long dns_elapsed_time) {
            this.dns_elapsed_time = dns_elapsed_time;
        }

        public void setConnect_elapsed_time(long connect_elapsed_time) {
            this.connect_elapsed_time = connect_elapsed_time;
        }

        public void setTls_connect_elapsed_time(long tls_connect_elapsed_time) {
            this.tls_connect_elapsed_time = tls_connect_elapsed_time;
        }

        public void setWait_elapsed_time(long wait_elapsed_time) {
            this.wait_elapsed_time = wait_elapsed_time;
        }

        public void setResponse_elapsed_time(long response_elapsed_time) {
            this.response_elapsed_time = response_elapsed_time;
        }

        public void setFile_offset(long file_offset) {
            this.file_offset = file_offset;
        }

        public void setBytes_sent(long bytes_sent) {
            this.bytes_sent = bytes_sent;
        }

        public void setBytes_total(long bytes_total) {
            this.bytes_total = bytes_total;
        }

        public void setPid(long pid) {
            this.pid = pid;
        }

        public void setTid(long tid) {
            this.tid = tid;
        }

        public void setTarget_region_id(String target_region_id) {
            this.target_region_id = target_region_id;
        }

        public void setError_type(String error_type) {
            this.error_type = error_type;
        }

        public void setError_description(String error_description) {
            this.error_description = error_description;
        }

        public void setOs_name(String os_name) {
            this.os_name = os_name;
        }

        public void setOs_version(String os_version) {
            this.os_version = os_version;
        }

        public void setSdk_name(String sdk_name) {
            this.sdk_name = sdk_name;
        }

        public void setSdk_version(String sdk_version) {
            this.sdk_version = sdk_version;
        }

        public void setUp_time(long up_time) {
            this.up_time = up_time;
        }

        public void setNetwork_type(String network_type) {
            this.network_type = network_type;
        }

        public void setSignal_strength(long signal_strength) {
            this.signal_strength = signal_strength;
        }

        public void setRequest_elapsed_time(long request_elapsed_time) {
            this.request_elapsed_time = request_elapsed_time;
        }

        public long getRequest_elapsed_time() {
            return request_elapsed_time;
        }


        public void setPrefetched_ip_count(long prefetched_ip_count) {
            this.prefetched_ip_count = prefetched_ip_count;
        }
    }

    public static class BlockInfo {
        private String log_type;
        private String target_region_id;
        private String current_region_id;
        private long total_elapsed_time;
        private long bytes_sent;
        /**
         * resumeUpload（bput）记录开始位置，resumeUploadFast多线程记录的是已上传的数据总量
         */
        private long recovered_from;
        private long file_size;
        private long pid;
        private long tid;
        private int up_api_version;
        private long up_time;

        public BlockInfo() {
            this.log_type = "block";
        }

        public String getLog_type() {
            return log_type;
        }

        public String getTarget_region_id() {
            return target_region_id;
        }

        public String getCurrent_region_id() {
            return current_region_id;
        }

        public long getTotal_elapsed_time() {
            return total_elapsed_time;
        }

        public long getBytes_sent() {
            return bytes_sent;
        }

        public long getRecovered_from() {
            return recovered_from;
        }

        public long getFile_size() {
            return file_size;
        }

        public long getPid() {
            return pid;
        }

        public long getTid() {
            return tid;
        }

        public int getUp_api_version() {
            return up_api_version;
        }

        public long getUp_time() {
            return up_time;
        }

        public void setLog_type(String log_type) {
            this.log_type = log_type;
        }

        public void setTarget_region_id(String target_region_id) {
            this.target_region_id = target_region_id;
        }

        public void setCurrent_region_id(String current_region_id) {
            this.current_region_id = current_region_id;
        }

        public void setTotal_elapsed_time(long total_elapsed_time) {
            this.total_elapsed_time = total_elapsed_time;
        }

        public void setBytes_sent(long bytes_sent) {
            this.bytes_sent = bytes_sent;
        }

        public void setRecovered_from(long recovered_from) {
            this.recovered_from = recovered_from;
        }

        public void setFile_size(long file_size) {
            this.file_size = file_size;
        }

        public void setPid(long pid) {
            this.pid = pid;
        }

        public void setTid(long tid) {
            this.tid = tid;
        }

        public void setUp_api_version(int up_api_version) {
            this.up_api_version = up_api_version;
        }

        public void setUp_time(long up_time) {
            this.up_time = up_time;
        }
    }

    public static class UploadQuality {
        private String log_type;
        private String result;
        private long total_elapsed_time;
        private long requests_counts;
        private long regions_counts;
        private long bytes_sent;
        private String cloud_type;
        private long up_time;

        UploadQuality() {
            this.log_type = "quality";
        }

        public void setLog_type(String log_type) {
            this.log_type = log_type;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public void setTotal_elapsed_time(long total_elapsed_time) {
            this.total_elapsed_time = total_elapsed_time;
        }

        public void setRequests_counts(long requests_counts) {
            this.requests_counts = requests_counts;
        }

        public void setRegions_counts(long regions_counts) {
            this.regions_counts = regions_counts;
        }

        public void setBytes_sent(long bytes_sent) {
            this.bytes_sent = bytes_sent;
        }

        public void setCloud_type(String cloud_type) {
            this.cloud_type = cloud_type;
        }

        public void setUp_time(long up_time) {
            this.up_time = up_time;
        }

        public String getLog_type() {
            return log_type;
        }

        public String getResult() {
            return result;
        }

        public long getTotal_elapsed_time() {
            return total_elapsed_time;
        }

        public long getRequests_counts() {
            return requests_counts;
        }

        public long getRegions_counts() {
            return regions_counts;
        }

        public long getBytes_sent() {
            return bytes_sent;
        }

        public String getCloud_type() {
            return cloud_type;
        }

        public long getUp_time() {
            return up_time;
        }

    }
}
