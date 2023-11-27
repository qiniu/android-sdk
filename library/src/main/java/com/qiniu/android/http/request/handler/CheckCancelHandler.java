package com.qiniu.android.http.request.handler;

/**
 * CheckCancelHandler
 */
public interface CheckCancelHandler {

    /**
     * 查看是否取消
     *
     * @return 是否取消
     */
    boolean checkCancel();
}
