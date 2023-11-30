package com.qiniu.android.http.metrics;

import com.qiniu.android.http.request.IUploadRegion;
import com.qiniu.android.utils.Utils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 区域上传事务
 *
 * @hidden
 */
public class UploadRegionRequestMetrics extends UploadMetrics {

    /**
     * 上传区域
     */
    public final IUploadRegion region;

    private List<UploadSingleRequestMetrics> metricsList = new CopyOnWriteArrayList<>();

    /**
     * 构造函数
     *
     * @param region 上传区域
     */
    public UploadRegionRequestMetrics(IUploadRegion region) {
        this.region = region;
    }

    /**
     * 请求的次数
     *
     * @return 请求的次数
     */
    public Integer requestCount() {
        return metricsList.size();
    }

    /**
     * 发送数据的大小
     *
     * @return 发送数据的大小
     */
    public Long bytesSend() {
        if (metricsList.isEmpty()) {
            return 0L;
        }
        long bytes = 0;
        for (UploadSingleRequestMetrics metrics : metricsList) {
            if (metrics != null) {
                bytes += metrics.bytesSend();
            }
        }
        return bytes;
    }

    /**
     * 获取最后一个请求指标
     *
     * @return 最后一个请求指标
     */
    public UploadSingleRequestMetrics lastMetrics() {
        int size = metricsList.size();
        return size < 1 ? null : metricsList.get(size - 1);
    }

    /**
     * 添加新的上传请求指标
     *
     * @param metricsList 新的上传请求指标
     */
    public void addMetricsList(List<UploadSingleRequestMetrics> metricsList) {
        if (metricsList == null || metricsList.size() == 0) {
            return;
        }
        for (UploadSingleRequestMetrics metrics : metricsList) {
            if (metrics != null) {
                this.metricsList.add(metrics);
            }
        }
    }

    /**
     * 添加新的上传请求指标
     *
     * @param metrics 新的上传请求指标
     */
    public void addMetrics(UploadRegionRequestMetrics metrics) {
        if (metrics == null || metrics.region == null || metrics.region.getZoneInfo() == null
                || metrics.region.getZoneInfo().regionId == null
                || region == null || region.getZoneInfo() == null || region.getZoneInfo().regionId == null
                || metrics.metricsList == null || metrics.metricsList.size() == 0) {

            return;
        }
        String thisRegionId = metrics.region.getZoneInfo().getRegionId();
        String metricsRegionId = metrics.region.getZoneInfo().getRegionId();
        if (thisRegionId.equals(metricsRegionId)) {
            // 拼接开始和结束时间，开始时间要最老的，结束时间要最新的
            if (startDate != null && metrics.startDate != null && startDate.getTime() > metrics.startDate.getTime()) {
                startDate = metrics.startDate;
            }
            if (endDate != null && metrics.endDate != null && endDate.getTime() < metrics.endDate.getTime()) {
                endDate = metrics.endDate;
            }

            addMetricsList(metrics.metricsList);
        }
    }
}
