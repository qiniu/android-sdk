package com.qiniu.android.http.metrics;

import com.qiniu.android.http.request.IUploadRegion;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class UploadRegionRequestMetrics {

    public final IUploadRegion region;
    private List<UploadSingleRequestMetrics> metricsList = new CopyOnWriteArrayList<>();

    public UploadRegionRequestMetrics(IUploadRegion region) {
        this.region = region;
    }

    public long totalElapsedTime(){
        if (metricsList.size() == 0){
            return 0l;
        }
        long time = 0;
        for (UploadSingleRequestMetrics metrics : metricsList){
            if (metrics != null){
                time += metrics.totalElapsedTime();
            }
        }
        return time;
    }

    public Integer requestCount(){
        return metricsList.size();
    }

    public Long bytesSend(){
        if (metricsList.size() == 0){
            return 0l;
        }
        long bytes = 0;
        for (UploadSingleRequestMetrics metrics : metricsList){
            if (metrics != null) {
                bytes += metrics.bytesSend();
            }
        }
        return bytes;
    }

    public void addMetricsList(List<UploadSingleRequestMetrics> metricsList){
        if (metricsList == null || metricsList.size() == 0){
            return;
        }
        for (UploadSingleRequestMetrics metrics : metricsList) {
            if (metrics != null){
                this.metricsList.add(metrics);
            }
        }
    }

    public void addMetrics(UploadRegionRequestMetrics metrics){
        if (metrics == null || metrics.region == null || metrics.region.getZoneInfo() == null
                || metrics.region.getZoneInfo().regionId == null
                || region == null || region.getZoneInfo() == null || region.getZoneInfo().regionId == null
                || metrics.metricsList == null || metrics.metricsList.size() == 0){

            return;
        }
        String thisRegionId = metrics.region.getZoneInfo().getRegionId();
        String metricsRegionId = metrics.region.getZoneInfo().getRegionId();
        if (thisRegionId.equals(metricsRegionId)){
            addMetricsList(metrics.metricsList);
        }
    }
}
