package com.qiniu.android.http.metrics;

import com.qiniu.android.http.request.IUploadRegion;
import com.qiniu.android.utils.Utils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class UploadRegionRequestMetrics extends UploadMetrics {

    public final IUploadRegion region;

    private List<UploadSingleRequestMetrics> metricsList = new CopyOnWriteArrayList<>();

    public UploadRegionRequestMetrics(IUploadRegion region) {
        this.region = region;
    }

    public Integer requestCount() {
        return metricsList.size();
    }

    public Long bytesSend() {
        if (metricsList.size() == 0) {
            return 0l;
        }
        long bytes = 0;
        for (UploadSingleRequestMetrics metrics : metricsList) {
            if (metrics != null) {
                bytes += metrics.bytesSend();
            }
        }
        return bytes;
    }

    public UploadSingleRequestMetrics lastMetrics() {
        int size = metricsList.size();
        return size < 1 ? null : metricsList.get(size - 1);
    }

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
