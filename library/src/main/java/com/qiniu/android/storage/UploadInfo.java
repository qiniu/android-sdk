package com.qiniu.android.storage;

import org.json.JSONObject;

import java.io.IOException;

abstract class UploadInfo {

    protected String sourceId;
    protected String fileName = null;
    protected long fileSize = -1;
    protected Configuration configuration;

    private UploadSource source;

    protected UploadInfo() {
    }

    UploadInfo(UploadSource source, Configuration configuration) {
        this.source = source;
        this.configuration = configuration;
        this.fileSize = source.getFileSize();
        this.sourceId = source.getId() != null ? source.getId() : "";
    }

    void setSource(UploadSource source) {
        this.source = source;
    }

    /**
     * 是否为同一个 UploadInfo
     * 同一个：source 相同，上传方式相同
     */
    boolean isSameUploadInfo(UploadInfo info) {
        return info != null && sourceId.equals(info.sourceId);
    }

    /**
     * 获取资源大小
     * @return
     */
    long getSourceSize() {
        if (fileSize > 0) {
            return fileSize;
        }
        return -1;
    }

    /**
     * 是否有效，为空则无效
     * @return 是否有效
     */
    boolean isValid() {
        return source != null && source.isValid();
    }

    /**
     * 上传进度
     * @return 上传进度
     */
    abstract double progress();

    /**
     * 是否已没有文件内容需要上传
     * @return return
     */
    abstract boolean isAllUploadingOrUploaded();

    /**
     * 文件内容是否完全上传完毕
     * @return 是否完全上传完毕
     */
    abstract boolean isAllUploaded();

    /**
     * 清楚上传状态
     */
    abstract void clearUploadState();

    /**
     * 转 json
     * @return json
     */
    abstract JSONObject toJsonObject();

    void close() {

    }

    byte[] readData(int dataSize, long dataOffset) throws IOException {
        if (source == null) {
            return null;
        }

        return source.readData(dataSize, dataOffset);
    }
}