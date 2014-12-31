package com.qiniu.android.storage;

/**
 * 定义用户取消数据或文件上传的信号
 */
public interface UpCancellationSignal {

    /**
     * 用户取消上传时，必须实现的方法
     *
     * @return 是否取消上传
     */
    boolean isCancelled();
}
