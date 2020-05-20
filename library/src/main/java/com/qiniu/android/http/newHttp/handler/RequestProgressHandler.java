package com.qiniu.android.http.newHttp.handler;

public interface RequestProgressHandler {
    public void progress(long totalBytesWritten, long totalBytesExpectedToWrite);
}
