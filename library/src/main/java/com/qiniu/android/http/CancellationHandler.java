package com.qiniu.android.http;

import java.io.IOException;

/**
 * 取消对调定义
 */
public interface CancellationHandler {

    /**
     * 定义用户取消数据或文件上传的信号
     *
     * @return 是否已取消
     */
    boolean isCancelled();

    /**
     * 取消异常
     */
    class CancellationException extends IOException {
    }
}
