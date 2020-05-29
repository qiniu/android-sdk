package com.qiniu.android.http.metrics;

import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.http.request.UploadRegion;

import java.util.ArrayList;
import java.util.HashMap;

public class UploadTaskMetrics {

    public ArrayList<UploadRegion> regions;
    private HashMap<String, UploadRegionRequestMetrics> metricsInfo;

    public UploadTaskMetrics(ArrayList<UploadRegion> regions) {
        this.regions = regions;
        this.metricsInfo = new HashMap<String, UploadRegionRequestMetrics>();
    }


    public Long totalElaspsedTime(){
        long time = 0;
        for (String key : metricsInfo.keySet()){
            UploadRegionRequestMetrics metrics = metricsInfo.get(key);
            time += metrics.totalElaspsedTime();
        }
        return time;
    }

    public Long requestCount(){
        long count = 0;
        for (String key : metricsInfo.keySet()){
            UploadRegionRequestMetrics metrics = metricsInfo.get(key);
            count += metrics.requestCount();
        }
        return count;
    }

    public Long bytesSend(){
        long bytesSend = 0;
        for (String key : metricsInfo.keySet()){
            UploadRegionRequestMetrics metrics = metricsInfo.get(key);
            bytesSend += metrics.bytesSend();
        }
        return bytesSend;
    }

    public Long regionCount(){
        long count = 0;
        for (String key : metricsInfo.keySet()){
            UploadRegionRequestMetrics metrics = metricsInfo.get(key);
            if (metrics.region != null && metrics.region.getZoneInfo() != null
                    && !metrics.region.getZoneInfo().regionId.equals(ZoneInfo.EmptyRegionId)){
                count += 1;
            }
        }
        return count;
    }

    public void addMetrics(UploadRegionRequestMetrics metrics){
        if (metrics == null || metrics.region == null || metrics.region.getZoneInfo() == null
                || metrics.region.getZoneInfo().regionId == null){
            return;
        }
        String regionId = metrics.region.getZoneInfo().regionId;
        UploadRegionRequestMetrics metricsOld = metricsInfo.get(regionId);
        if (metricsOld != null){
            metricsOld.addMetrics(metrics);
        } else {
            metricsInfo.put(regionId, metrics);
        }
    }
}
