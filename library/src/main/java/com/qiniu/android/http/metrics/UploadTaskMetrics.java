package com.qiniu.android.http.metrics;

import com.qiniu.android.http.request.UploadRegion;

import java.util.ArrayList;

public class UploadTaskMetrics {

    public final ArrayList<UploadRegion> regions;
    private ArrayList<UploadRegionRequestMetrics> metricsList;

    public UploadTaskMetrics(ArrayList<UploadRegion> regions) {
        this.regions = regions;
        this.metricsList = new ArrayList<UploadRegionRequestMetrics>();
    }


    public Long totalElaspsedTime(){
        return null;
    }

    public Long requestCount(){
        return null;
    }

    public Long bytesSend(){
        return null;
    }

    public Long regionCount(){
        return null;
    }

    public ArrayList<UploadRegionRequestMetrics> getMetricsList(){
        return metricsList;
    }

    public void addMetrics(UploadRegionRequestMetrics metrics){
        metricsList.add(metrics);
    }
}
