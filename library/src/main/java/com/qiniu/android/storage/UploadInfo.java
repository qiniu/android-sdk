package com.qiniu.android.storage;

import org.json.JSONObject;

import java.io.IOException;

abstract class UploadInfo {

    protected String sourceId;
    protected String fileName = null;
    protected long sourceSize = UploadSource.UnknownSourceSize;
    protected Configuration configuration;

    private UploadSource source;

    protected UploadInfo() {
    }

    UploadInfo(UploadSource source, Configuration configuration) {
        this.source = source;
        this.configuration = configuration;
        this.sourceSize = source.getSize();
        this.sourceId = source.getId() != null ? source.getId() : "";
    }

    void setSource(UploadSource source) {
        this.source = source;
    }

    /**
     * 是否可以重新加载文件信息，也即是否可以重新读取信息
     * @return return
     */
    boolean couldReloadInfo() {
        return source.couldReloadInfo();
    }

    /**
     * 重新加载文件信息，以便于重新读取
     *
     * @return 重新加载是否成功
     */
    boolean reloadInfo() {
        return source.reloadInfo();
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
     * 获取资源大小
     * @return
     */
    long getSourceSize() {
        if (sourceSize < 0) {
            sourceSize = source.getSize();
        }
        return sourceSize;
    }

    /**
     * 数据源是否有效，为空则无效
     * @return 是否有效
     */
    boolean hasValidResource() {
        return source != null;
    }

    /**
     * 是否有效
     * @return 是否有效
     */
    boolean isValid() {
        return hasValidResource();
    }

    /**
     * 获取已上传数据的大小
     * @return 已上传数据的大小
     */
    abstract long uploadSize();

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
