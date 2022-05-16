package com.qiniu.android.storage;

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

    static UploadData dataFromJson(JSONObject jsonObject) throws Exception {
        if (jsonObject == null) {
            return null;
        }

        long offset = jsonObject.getLong("offset");
        int size = jsonObject.getInt("size");
        int index = jsonObject.getInt("index");
        String etag = jsonObject.optString("etag");
        State state = State.state(jsonObject.getInt("state"));
        String md5 = jsonObject.optString("md5");

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

    // 是否已经上传
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
        md5 = null;
        state = State.NeedToCheck;
    }

    void checkStateAndUpdate() {
        if ((state == State.WaitToUpload || state == State.Uploading) && data == null) {
            state = State.NeedToCheck;
        }
    }

    JSONObject toJsonObject() throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.putOpt("offset", offset);
        jsonObject.putOpt("size", size);
        jsonObject.putOpt("index", index);
        jsonObject.putOpt("etag", etag);
        jsonObject.putOpt("md5", md5);
        jsonObject.putOpt("state", state.intValue());
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
