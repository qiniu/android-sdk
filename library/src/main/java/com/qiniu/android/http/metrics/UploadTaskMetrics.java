package com.qiniu.android.http.metrics;

import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.http.request.IUploadRegion;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class UploadTaskMetrics extends UploadMetrics {

    public ArrayList<IUploadRegion> regions;

    private String upType;
    private UploadRegionRequestMetrics ucQueryMetrics;
    private List<String> metricsKeys = new CopyOnWriteArrayList<>();
    private Map<String, UploadRegionRequestMetrics> metricsInfo = new ConcurrentHashMap<>();

    public UploadTaskMetrics(String upType) {
        this.upType = upType;
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

    public UploadRegionRequestMetrics lastMetrics() {
        int size = metricsKeys.size();
        if (size < 1) {
            return null;
        }

        String key = metricsKeys.get(size - 1);
        return metricsInfo.get(key);
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
            metricsKeys.add(regionId);
            metricsInfo.put(regionId, metrics);
        }
    }

    public String getUpType() {
        return upType;
    }

    public UploadRegionRequestMetrics getUcQueryMetrics() {
        return ucQueryMetrics;
    }

    public void setUcQueryMetrics(UploadRegionRequestMetrics ucQueryMetrics) {
        this.ucQueryMetrics = ucQueryMetrics;
        addMetrics(ucQueryMetrics);
    }
}
