package com.qiniu.android.storage;

public interface UpProgressBytesHandler extends UpProgressHandler {
    /**
     * 用户自定义进度处理类必须实现的方法
     * 注：
     * 使用此接口，{@link UpProgressHandler#progress(String, double)} 会无效
     *
     * @param key         上传文件的保存文件名
     * @param uploadBytes 已上传大小
     * @param totalBytes  总大小；无法获取大小时为 -1
     */
    void progress(String key, long uploadBytes, long totalBytes);
}
