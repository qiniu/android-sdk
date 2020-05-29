package com.qiniu.android.http.request;

public class UploadRequestInfo {

    public static final String RequestTypeUCQuery = "uc_query";
    public static final String RequestTypeForm = "form";
    public static final String RequestTypeMkblk = "mkblk";
    public static final String RequestTypeBput = "bput";
    public static final String RequestTypeMkfile = "mkfile";
    public static final String RequestTypeUpLog = "uplog";

    public String requestType;
    public String bucket;
    public String key;
    public Long fileOffset;
    public String targetRegionId;
    public String currentRegionId;

    public boolean shouldReportRequestLog(){
        if (requestType.equals(RequestTypeUpLog)){
            return false;
        } else {
            return true;
        }
    }
}
