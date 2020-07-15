package com.qiniu.android.http.request;

class UploadRequestInfo {

    protected static final String RequestTypeUCQuery = "uc_query";
    protected static final String RequestTypeForm = "form";
    protected static final String RequestTypeMkblk = "mkblk";
    protected static final String RequestTypeBput = "bput";
    protected static final String RequestTypeMkfile = "mkfile";
    protected static final String RequestTypeUpLog = "uplog";

    protected String requestType;
    protected String bucket;
    protected String key;
    protected Long fileOffset;
    protected String targetRegionId;
    protected String currentRegionId;

    protected boolean shouldReportRequestLog(){
        return requestType.equals(RequestTypeUpLog);
    }
}
