package com.qiniu.android.storage;

import org.json.JSONException;
import org.json.JSONObject;

class UploadData {

    final long offset;
    final int size;
    final int index;

    String md5;
    String etag;

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
        State state = State.NeedToCheck;
        String md5 = null;
        try {
            offset = jsonObject.getLong("offset");
            size = jsonObject.getInt("size");
            index = jsonObject.getInt("index");
            etag = jsonObject.optString("etag");
            md5 = jsonObject.optString("md5");
            state = State.state(jsonObject.getInt("state"));
        } catch (JSONException ignored) {
        }
        UploadData uploadData = new UploadData(offset, size, index);
        uploadData.etag = etag;
        uploadData.md5 = md5;
        uploadData.state = state;
        uploadData.uploadSize = 0;
        return uploadData;
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
        state = State.WaitToUpload;
    }

    JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.putOpt("offset", offset);
            jsonObject.putOpt("size", size);
            jsonObject.putOpt("index", index);
            jsonObject.putOpt("etag", etag);
            jsonObject.putOpt("md5", md5);
            jsonObject.putOpt("state", state.intValue());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    enum State {
        NeedToCheck, // 需要检测数据
        WaitToUpload, // 等待上传
        Uploading, // 正在上传
        Complete; // 上传结束

        private int intValue() {
            return this.ordinal();
        }

        private static State state(int value) {
            State[] states = State.values();
            if (value < 0 || value >= states.length) {
                return NeedToCheck;
            } else {
                return states[value];
            }
        }
    }
}
