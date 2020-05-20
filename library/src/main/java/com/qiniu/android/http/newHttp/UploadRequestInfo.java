package com.qiniu.android.http.newHttp;

public class UploadRequestInfo {

    public static final String RequestTypeUCQuery = "uc_query";
    public static final String RequestTypeForm = "form";
    public static final String RequestTypeMkblk = "mkblk";
    public static final String RequestTypeBput = "bput";
    public static final String RequestTypeMkfile = "mkfile";

    public String requestType;
    public String bucket;
    public String key;
    public Long fileOffset;
    public String targetRegionId;
    public String currentRegionId;
}
