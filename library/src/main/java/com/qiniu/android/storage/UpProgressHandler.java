package com.qiniu.android.storage;

/**
 * 定义数据或文件上传的进度处理方法
 */
public interface UpProgressHandler {

    /**
     * 用户自定义进度处理类必须实现的方法
     *
     * @param key     上传文件的保存文件名
     * @param percent 上传进度，取值范围[0, 1.0]
     */
    void progress(String key, double percent);
}
