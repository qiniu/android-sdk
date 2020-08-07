package com.qiniu.android.http.metrics;

import com.qiniu.android.http.request.IUploadRegion;

import java.util.ArrayList;

public class UploadRegionRequestMetrics {

    public final IUploadRegion region;
    private ArrayList <UploadSingleRequestMetrics> metricsList = new ArrayList<>();

    public UploadRegionRequestMetrics(IUploadRegion region) {
        this.region = region;
    }

    public Long totalElapsedTime(){
        if (metricsList.size() == 0){
            return 0l;
        }
        long time = 0;
        for (UploadSingleRequestMetrics metrics : metricsList){
            time += metrics.totalElapsedTime();
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
            bytes += metrics.bytesSend();
        }
        return bytes;
    }

    public void addMetricsList(ArrayList<UploadSingleRequestMetrics> metricsList){
        this.metricsList.addAll(0, metricsList);
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
            metricsList.addAll(0, metrics.metricsList);
        }
    }
}
