package com.qiniu.android.http.request.handler;

/**
 * 请求进度回调
 */
public interface RequestProgressHandler {

    /**
     * 请求进度回调
     *
     * @param totalBytesWritten         已发送数据大小
     * @param totalBytesExpectedToWrite 总数据大小
     */
    void progress(long totalBytesWritten, long totalBytesExpectedToWrite);
}
