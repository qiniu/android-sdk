package com.qiniu.android.storage;


public interface UpProgressHandler {
    void progress(String key, double percent);
}
