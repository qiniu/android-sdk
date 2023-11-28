package com.qiniu.android.http.metrics;

import com.qiniu.android.utils.Utils;

import java.util.Date;

/**
 * 上传指标
 */
public class UploadMetrics {

    /**
     * 开始时间
     */
    protected Date startDate = null;

    /**
     * 结束时间
     */
    protected Date endDate = null;

    /**
     * 构造函数
     */
    protected UploadMetrics() {
    }

    /**
     * 开始
     */
    public void start() {
        startDate = new Date();
    }

    /**
     * 结束
     */
    public void end() {
        endDate = new Date();
    }

    /**
     * 获取开始时间
     *
     * @return 开始时间
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * 获取总耗时
     *
     * @return 总耗时
     */
    public long totalElapsedTime() {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return Utils.dateDuration(startDate, endDate);
    }
}
