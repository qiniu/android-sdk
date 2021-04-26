package com.qiniu.android.storage;

public interface UpProgressBytesHandler extends UpProgressHandler {
    /**
     * 用户自定义进度处理类必须实现的方法
     *
     * @param key         上传文件的保存文件名
     * @param uploadBytes 已上传大小
     * @param totalBytes  总大小，InputStream, 无法获取大小
     */
    void progress(String key, long uploadBytes, long totalBytes);
}
