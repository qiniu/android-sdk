package com.qiniu.android.http.metrics;

import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.http.request.IUploadRegion;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UploadTaskMetrics extends UploadMetrics {

    public ArrayList<IUploadRegion> regions;
    private Map<String, UploadRegionRequestMetrics> metricsInfo;

    public UploadTaskMetrics(ArrayList<IUploadRegion> regions) {
        this.regions = regions;
        this.metricsInfo = new ConcurrentHashMap<>();
    }

    public Long requestCount(){
        long count = 0;
        for (String key : metricsInfo.keySet()){
            UploadRegionRequestMetrics metrics = metricsInfo.get(key);
            if (metrics != null){
                count += metrics.requestCount();
            }
        }
        return count;
    }

    public Long bytesSend(){
        long bytesSend = 0;
        for (String key : metricsInfo.keySet()){
            UploadRegionRequestMetrics metrics = metricsInfo.get(key);
            if (metrics != null){
                bytesSend += metrics.bytesSend();
            }
        }
        return bytesSend;
    }

    public Long regionCount(){
        long count = 0;
        for (String key : metricsInfo.keySet()){
            UploadRegionRequestMetrics metrics = metricsInfo.get(key);
            if (metrics !=null
                    && metrics.region != null && metrics.region.getZoneInfo() != null
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
