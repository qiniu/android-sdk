package com.qiniu.android.http.metrics;

import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.http.request.IUploadRegion;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * upload task metrics
 */
public class UploadTaskMetrics extends UploadMetrics {

    /**
     * 上传的 regions
     */
    public ArrayList<IUploadRegion> regions;

    private String upType;
    private UploadRegionRequestMetrics ucQueryMetrics;
    private List<String> metricsKeys = new CopyOnWriteArrayList<>();
    private Map<String, UploadRegionRequestMetrics> metricsInfo = new ConcurrentHashMap<>();

    /**
     * 构造函数
     *
     * @param upType 上传类型
     */
    public UploadTaskMetrics(String upType) {
        this.upType = upType;
    }

    /**
     * 获取请求次数
     *
     * @return 请求次数
     */
    public Long requestCount() {
        long count = 0;
        for (String key : metricsInfo.keySet()) {
            UploadRegionRequestMetrics metrics = metricsInfo.get(key);
            if (metrics != null) {
                count += metrics.requestCount();
            }
        }
        return count;
    }

    /**
     * 获取发送数据大小
     *
     * @return 发送数据大小
     */
    public Long bytesSend() {
        long bytesSend = 0;
        for (String key : metricsInfo.keySet()) {
            UploadRegionRequestMetrics metrics = metricsInfo.get(key);
            if (metrics != null) {
                bytesSend += metrics.bytesSend();
            }
        }
        return bytesSend;
    }

    /**
     * 获取上传使用区域个数
     *
     * @return 上传使用区域个数
     */
    public Long regionCount() {
        long count = 0;
        for (String key : metricsInfo.keySet()) {
            UploadRegionRequestMetrics metrics = metricsInfo.get(key);
            if (metrics != null
                    && metrics.region != null && metrics.region.getZoneInfo() != null
                    && !metrics.region.getZoneInfo().regionId.equals(ZoneInfo.EmptyRegionId)) {
                count += 1;
            }
        }
        return count;
    }

    /**
     * 获取最后一个区域请求指标
     *
     * @return 最后一个区域请求指标
     */
    public UploadRegionRequestMetrics lastMetrics() {
        int size = metricsKeys.size();
        if (size < 1) {
            return null;
        }

        String key = metricsKeys.get(size - 1);
        return metricsInfo.get(key);
    }

    /**
     * 添加区域请求指标
     *
     * @param metrics 区域请求指标
     */
    public void addMetrics(UploadRegionRequestMetrics metrics) {
        if (metrics == null || metrics.region == null || metrics.region.getZoneInfo() == null
                || metrics.region.getZoneInfo().regionId == null) {
            return;
        }
        String regionId = metrics.region.getZoneInfo().regionId;
        UploadRegionRequestMetrics metricsOld = metricsInfo.get(regionId);
        if (metricsOld != null) {
            metricsOld.addMetrics(metrics);
        } else {
            metricsKeys.add(regionId);
            metricsInfo.put(regionId, metrics);
        }
    }

    /**
     * 获取上传类型
     *
     * @return 上传类型
     */
    public String getUpType() {
        return upType;
    }

    /**
     * 获取 uc query 请求指标
     *
     * @return uc query 请求指标
     */
    public UploadRegionRequestMetrics getUcQueryMetrics() {
        return ucQueryMetrics;
    }

    /**
     * 设置 uc query 请求指标
     *
     * @param ucQueryMetrics uc query 请求指标
     */
    public void setUcQueryMetrics(UploadRegionRequestMetrics ucQueryMetrics) {
        this.ucQueryMetrics = ucQueryMetrics;
        addMetrics(ucQueryMetrics);
    }
}
