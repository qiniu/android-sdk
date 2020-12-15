package com.qiniu.android.storage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class UploadFileInfo {

    final long size;
    final long modifyTime;

    UploadFileInfo(long fileSize,
                   long modifyTime) {
        this.size = fileSize;
        this.modifyTime = modifyTime;
    }

    abstract UploadFileInfo fileFromJson(JSONObject jsonObject);

    double progress() {
        return 0;
    }

    abstract boolean isEmpty();

    abstract boolean isValid();

    abstract void clearUploadState();

    abstract boolean isAllUploaded();

    abstract JSONObject toJsonObject();
}
