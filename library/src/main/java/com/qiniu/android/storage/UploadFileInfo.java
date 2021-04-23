package com.qiniu.android.storage;

import org.json.JSONObject;

abstract class UploadFileInfo {

    final long size;
    final long modifyTime;

    UploadFileInfo(long fileSize, long modifyTime) {
        this.size = fileSize;
        this.modifyTime = modifyTime;
    }

    abstract double progress();

    abstract boolean isEmpty();

    abstract boolean isValid();

    abstract void clearUploadState();

    abstract boolean isAllUploaded();

    abstract JSONObject toJsonObject();
}
