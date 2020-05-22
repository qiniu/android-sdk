package com.qiniu.android.http.metrics;

import com.qiniu.android.http.request.UploadRegion;

import java.util.ArrayList;

public class UploadRegionRequestMetrics {

    private final UploadRegion region;
    private ArrayList <UploadSingleRequestMetrics> metricsList;

    public UploadRegionRequestMetrics(UploadRegion region) {
        this.region = region;
    }

    public Long totalElaspsedTime(){
        return null;
    }

    public Integer requestCount(){
        return null;
    }

    public Long bytesSend(){
        return null;
    }

    public void addMetricsList(ArrayList<UploadSingleRequestMetrics> metricsList){
        metricsList.addAll(0, metricsList);
    }
}
