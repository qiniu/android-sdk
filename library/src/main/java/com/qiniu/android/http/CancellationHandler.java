package com.qiniu.android.http;

import java.io.IOException;

public interface CancellationHandler {

    /**
     * 定义用户取消数据或文件上传的信号
     */
    boolean isCancelled();

    public static class CancellationException extends IOException {

    }
}
