package com.qiniu.android.storage;

import org.json.JSONException;
import org.json.JSONObject;

class UploadData {

    final long offset;
    final int size;
    final int index;

    String etag;
    boolean isCompleted;
    boolean isUploading;
    double progress;

    byte[] data;

    UploadData(long offset, int size, int index) {
        this.offset = offset;
        this.size = size;
        this.index = index;
        this.isCompleted = false;
        this.isUploading = false;
        this.progress = 0;
    }

    static UploadData dataFromJson(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        long offset = 0;
        int size = 0;
        int index = 0;
        String etag = null;
        boolean isCompleted = false;
        double progress = 0;
        try {
            offset = jsonObject.getLong("offset");
            size = jsonObject.getInt("size");
            index = jsonObject.getInt("index");
            isCompleted = jsonObject.getBoolean("isCompleted");
            progress = jsonObject.getDouble("progress");
            etag = jsonObject.getString("etag");
        } catch (JSONException ignored) {
        }
        UploadData uploadData = new UploadData(offset, size, index);
        uploadData.isCompleted = isCompleted;
        uploadData.progress = progress;
        uploadData.etag = etag;
        return uploadData;
    }

    boolean isFirstData() {
        return index == 1;
    }

    void clearUploadState() {
        etag = null;
        isCompleted = false;
        isUploading = false;
    }

    JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("offset", offset);
            jsonObject.put("size", size);
            jsonObject.put("index", index);
            jsonObject.put("isCompleted", isCompleted);
            jsonObject.put("progress", progress);
            jsonObject.put("etag", etag);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
