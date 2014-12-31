package com.qiniu.android.http;

/**
 * 定义进度处理接口
 */
public interface ProgressHandler {
    /**
     * 用户自定义进度处理对象必须实现的接口方法
     */
    void onProgress(int bytesWritten, int totalSize);
}
