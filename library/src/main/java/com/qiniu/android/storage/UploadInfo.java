package com.qiniu.android.storage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

abstract class UploadInfo {

    private String sourceId;
    private long sourceSize = UploadSource.UnknownSourceSize;
    protected String fileName = null;

    private UploadSource source;

    UploadInfo(UploadSource source) {
        this.source = source;
        this.sourceSize = source.getSize();
        this.sourceId = source.getId() != null ? source.getId() : "";
    }

    void setInfoFromJson(JSONObject jsonObject) {
        try {
            sourceSize = jsonObject.getLong("sourceSize");
            sourceId = jsonObject.optString("sourceId");
        } catch (JSONException ignored) {
        }
    }

    /**
     * 是否可以重新加载文件信息，也即是否可以重新读取信息
     *
     * @return return
     */
    boolean couldReloadSource() {
        return source.couldReloadSource();
    }

    /**
     * 重新加载文件信息，以便于重新读取
     *
     * @return 重新加载是否成功
     */
    boolean reloadSource() {
        return source.reloadSource();
    }

    /**
     * 是否为同一个 UploadInfo
     * 同一个：source 相同，上传方式相同
     */
    boolean isSameUploadInfo(UploadInfo info) {
        if (info == null || !sourceId.equals(info.sourceId)) {
            return false;
        }

        // 检测文件大小，如果能获取到文件大小的话，就进行检测
        if (info.sourceSize > UploadSource.UnknownSourceSize &&
                sourceSize > UploadSource.UnknownSourceSize &&
                info.sourceSize != sourceSize) {
            return false;
        }

        return true;
    }

    /**
     * 获取资源 id
     *
     * @return 资源 id
     */
    String getSourceId() {
        return sourceId;
    }

    /**
     * 获取资源大小
     *
     * @return
     */
    long getSourceSize() {
        return source.getSize();
    }

    /**
     * 数据源是否有效，为空则无效
     *
     * @return 是否有效
     */
    boolean hasValidResource() {
        return source != null;
    }

    /**
     * 是否有效
     *
     * @return 是否有效
     */
    boolean isValid() {
        return hasValidResource();
    }

    /**
     * 获取已上传数据的大小
     *
     * @return 已上传数据的大小
     */
    abstract long uploadSize();

    /**
     * 文件内容是否完全上传完毕
     *
     * @return 是否完全上传完毕
     */
    abstract boolean isAllUploaded();

    /**
     * 清楚上传状态
     */
    abstract void clearUploadState();

    /**
     * 检查文件状态, 主要处理没有 data 但处于上传状态
     */
    abstract void checkInfoStateAndUpdate();

    /**
     * 转 json
     *
     * @return json
     */
    JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("sourceId", sourceId);
            jsonObject.put("sourceSize", getSourceSize());
        } catch (JSONException ignore) {
        }
        return jsonObject;
    }

    void close() {
        source.close();
    }

    byte[] readData(int dataSize, long dataOffset) throws IOException {
        if (source == null) {
            throw new IOException("file is not exist");
        }

        byte[] data = null;
        synchronized (source) {
            data = source.readData(dataSize, dataOffset);
        }
        if (data != null && (data.length != dataSize || data.length == 0)) {
            sourceSize = dataOffset + data.length;
        }
        return data;
    }
}
