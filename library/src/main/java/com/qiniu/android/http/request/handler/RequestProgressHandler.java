package com.qiniu.android.http.request.handler;

public interface RequestProgressHandler {
    public void progress(long totalBytesWritten, long totalBytesExpectedToWrite);
}
