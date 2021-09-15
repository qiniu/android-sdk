package com.qiniu.android.http.request;

class UploadRequestInfo {

    static final String RequestTypeUCQuery = "uc_query";
    static final String RequestTypeForm = "form";
    static final String RequestTypeMkblk = "mkblk";
    static final String RequestTypeBput = "bput";
    static final String RequestTypeMkfile = "mkfile";
    static final String RequestTypeInitParts = "init_parts";
    static final String RequestTypeUploadPart = "upload_part";
    static final String RequestTypeCompletePart = "complete_part";
    static final String RequestTypeUpLog = "uplog";
    static final String RequestTypeServerConfig = "server_config";
    static final String RequestTypeServerUserConfig = "server_user_config";

    String requestType;
    String bucket;
    String key;
    Long fileOffset;
    String targetRegionId;
    String currentRegionId;

    boolean shouldReportRequestLog(){
        return !requestType.equals(RequestTypeUpLog);
    }
}
