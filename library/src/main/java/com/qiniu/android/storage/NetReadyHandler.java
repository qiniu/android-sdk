package com.qiniu.android.storage;

/**
 * Created by bailong on 16/9/7.
 */
public interface NetReadyHandler {

    /**
     * 等待网络正常连接
     */
    void waitReady();
}
