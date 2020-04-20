package com.qiniu.android.collect;

public class UploadInfo {

    public static UploadInfoElement.ReqInfo getReqInfo() {
        return new UploadInfoElement.ReqInfo();
    }

    public static UploadInfoElement.BlockInfo getBlockInfo() {
        return new UploadInfoElement.BlockInfo();
    }

    public static UploadInfoElement.UploadQuality getUploadQuality() {
        return new UploadInfoElement.UploadQuality();
    }
}
