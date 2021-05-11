package com.qiniu.android.storage;

import org.json.JSONException;
import org.json.JSONObject;

class UploadData {

    final long offset;
    final int size;
    final int index;

    String md5;
    String etag;
    String ctx;

    private State state;
    private long uploadSize = 0;

    byte[] data;

    UploadData(long offset, int size, int index) {
        this.offset = offset;
        this.size = size;
        this.index = index;
        this.state = State.NeedToCheck;
        this.uploadSize = 0;
    }

    static UploadData dataFromJson(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        long offset = 0;
        int size = 0;
        int index = 0;
        String etag = null;
        String ctx = null;
        String md5 = null;
        try {
            offset = jsonObject.getLong("offset");
            size = jsonObject.getInt("size");
            index = jsonObject.getInt("index");
            etag = jsonObject.optString("etag");
            ctx = jsonObject.optString("ctx");
            md5 = jsonObject.optString("md5");
        } catch (JSONException ignored) {
        }
        UploadData uploadData = new UploadData(offset, size, index);
        uploadData.ctx = ctx;
        uploadData.etag = etag;
        uploadData.md5 = md5;
        uploadData.uploadSize = 0;
        return uploadData;
    }

    boolean isFirstData() {
        return index == 1;
    }

    // 需要上传，但需要检测块信息是否有效
    boolean needToUpload() {
        switch (state) {
            case NeedToCheck:
            case WaitToUpload:
                return true;
            default:
                return false;
        }
    }

    // 需要上传，但是未上传
    boolean isUploaded() {
        return state == State.Complete;
    }

    State getState() {
        return state;
    }

    void updateState(State state) {
        switch (state) {
            case NeedToCheck:
            case WaitToUpload:
            case Uploading:
                uploadSize = 0;
                etag = null;
                ctx = null;
                break;
            case Complete:
                data = null;
        }
        this.state = state;
    }

    void setUploadSize(long uploadSize) {
        this.uploadSize = uploadSize;
    }

    long uploadSize() {
        return state == State.Complete ? size : uploadSize;
    }

    void clearUploadState() {
        etag = null;
        ctx = null;
        state = State.WaitToUpload;
    }

    JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.putOpt("offset", offset);
            jsonObject.putOpt("size", size);
            jsonObject.putOpt("index", index);
            jsonObject.putOpt("etag", etag);
            jsonObject.putOpt("ctx", ctx);
            jsonObject.putOpt("md5", md5);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    enum State {
        NeedToCheck, // 需要检测数据
        WaitToUpload, // 等待上传
        Uploading, // 正在上传
        Complete, // 上传结束
    }
}
