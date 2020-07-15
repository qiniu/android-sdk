package com.qiniu.android.http.request.handler;

public interface RequestProgressHandler {
    void progress(long totalBytesWritten, long totalBytesExpectedToWrite);
}
