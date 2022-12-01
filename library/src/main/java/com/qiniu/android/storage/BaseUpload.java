package com.qiniu.android.storage;

import com.qiniu.android.common.Zone;
import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.common.ZonesInfo;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.request.IUploadRegion;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.http.metrics.UploadTaskMetrics;
import com.qiniu.android.http.serverRegion.UploadDomainRegion;

import org.json.JSONObject;

import java.util.ArrayList;

abstract class BaseUpload implements Runnable {
    protected final String key;
    protected final String fileName;
    protected final byte[] data;
    protected final UploadSource uploadSource;
    protected final UpToken token;
    protected final UploadOptions option;
    protected final Configuration config;
    protected final Recorder recorder;
    protected final String recorderKey;
    protected final UpTaskCompletionHandler completionHandler;


    private UploadRegionRequestMetrics currentRegionRequestMetrics;
    private UploadTaskMetrics metrics;

    private int currentRegionIndex;
    private ArrayList<IUploadRegion> regions;

    private BaseUpload(UploadSource source,
                       byte[] data,
                       String fileName,
                       String key,
                       UpToken token,
                       UploadOptions option,
                       Configuration config,
                       Recorder recorder,
                       String recorderKey,
                       UpTaskCompletionHandler completionHandler) {
        this.uploadSource = source;
        this.data = data;
        this.fileName = fileName != null ? fileName : "?";
        this.key = key;
        this.token = token;
        this.option = option != null ? option : UploadOptions.defaultOptions();
        this.config = config;
        this.recorder = recorder;
        this.recorderKey = recorderKey;
        this.completionHandler = completionHandler;

        this.initData();
    }

    protected BaseUpload(UploadSource source,
                         String key,
                         UpToken token,
                         UploadOptions option,
                         Configuration config,
                         Recorder recorder,
                         String recorderKey,
                         UpTaskCompletionHandler completionHandler) {
        this(source, null, source.getFileName(), key, token, option, config, recorder, recorderKey, completionHandler);
    }

    protected BaseUpload(byte[] data,
                         String key,
                         String fileName,
                         UpToken token,
                         UploadOptions option,
                         Configuration config,
                         UpTaskCompletionHandler completionHandler) {
        this(null, data, fileName, key, token, option, config, null, null, completionHandler);
    }

    protected void initData() {
        currentRegionIndex = 0;
        metrics = new UploadTaskMetrics(getUpType());
    }


    @Override
    public void run() {
        metrics.start();

        config.zone.preQuery(token, new Zone.QueryHandler() {
            @Override
            public void complete(int code, ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics) {
                metrics.setUcQueryMetrics(requestMetrics);

                if (code == 0) {
                    int prepareCode = prepareToUpload();
                    if (prepareCode == 0) {
                        startToUpload();
                    } else {
                        ResponseInfo responseInfoP = ResponseInfo.errorInfo(prepareCode, null);
                        completeAction(responseInfoP, null);
                    }
                } else {
                    completeAction(responseInfo, responseInfo.response);
                }
            }
        });
    }

    protected boolean reloadUploadInfo() {
        return true;
    }

    protected int prepareToUpload() {
        int ret = 0;
        if (!setupRegions()) {
            ret = -1;
        }
        return ret;
    }

    protected void startToUpload() {
        currentRegionRequestMetrics = new UploadRegionRequestMetrics(getCurrentRegion());
        currentRegionRequestMetrics.start();
    }

    @Deprecated
    protected boolean switchRegionAndUpload() {
        if (currentRegionRequestMetrics != null) {
            currentRegionRequestMetrics.end();
            metrics.addMetrics(currentRegionRequestMetrics);
            currentRegionRequestMetrics = null;
        }

        boolean isSwitched = switchRegion();
        if (isSwitched) {
            startToUpload();
        }
        return isSwitched;
    }

    protected void completeAction(ResponseInfo responseInfo, JSONObject response) {
        if (metrics != null) {
            metrics.end();
        }
        if (currentRegionRequestMetrics != null) {
            currentRegionRequestMetrics.end();
        }

        if (currentRegionRequestMetrics != null && metrics != null) {
            metrics.addMetrics(currentRegionRequestMetrics);
        }

        if (completionHandler != null) {
            completionHandler.complete(responseInfo, key, metrics, response);
        }
    }

    private boolean setupRegions() {
        if (config == null || config.zone == null) {
            return false;
        }
        ZonesInfo zonesInfo = config.zone.getZonesInfo(token);
        if (zonesInfo == null || zonesInfo.zonesInfo == null || zonesInfo.zonesInfo.size() == 0) {
            return false;
        }
        ArrayList<ZoneInfo> zoneInfos = zonesInfo.zonesInfo;

        ArrayList<IUploadRegion> defaultRegions = new ArrayList<>();
        for (ZoneInfo zoneInfo : zoneInfos) {
            UploadDomainRegion region = new UploadDomainRegion();
            region.setupRegionData(zoneInfo);
            if (region.isValid()) {
                defaultRegions.add(region);
            }
        }
        regions = defaultRegions;
        metrics.regions = defaultRegions;
        return defaultRegions.size() > 0;
    }

    protected void insertRegionAtFirst(IUploadRegion region) {
        if (region == null) {
            return;
        }

        boolean hasRegion = false;
        for (IUploadRegion regionP : regions) {
            if (region.isEqual(regionP)) {
                hasRegion = true;
                break;
            }
        }
        if (!hasRegion) {
            regions.add(0, region);
        }
    }

    protected boolean switchRegion() {

        if (regions == null) {
            return false;
        }
        boolean ret = false;
        synchronized (this) {
            int regionIndex = currentRegionIndex + 1;
            if (regionIndex < regions.size()) {
                currentRegionIndex = regionIndex;
                ret = true;
            }
        }
        return ret;
    }

    protected boolean switchRegionAndUploadIfNeededWithErrorResponse(ResponseInfo errorResponseInfo) {
        if (errorResponseInfo == null || errorResponseInfo.isOK() || // 不存在 || 不是error 不切
                !errorResponseInfo.couldRetry() || !config.allowBackupHost) { // 不能重试
            return false;
        }

        if (currentRegionRequestMetrics != null) {
            currentRegionRequestMetrics.end();
            metrics.addMetrics(currentRegionRequestMetrics);
            currentRegionRequestMetrics = null;
        }

        // 重新加载上传数据，上传记录 & Resource index 归零
        if (!reloadUploadInfo()) {
            return false;
        }

        // 切换区域，当为 context 过期错误不需要切换区域
        if (!errorResponseInfo.isCtxExpiredError() && !switchRegion()) {
            // 非 context 过期错误，但是切换 region 失败
            return false;
        }

        startToUpload();

        return true;
    }

    protected IUploadRegion getTargetRegion() {
        if (regions == null || regions.size() == 0) {
            return null;
        } else {
            return regions.get(0);
        }
    }

    protected IUploadRegion getCurrentRegion() {
        if (regions == null) {
            return null;
        }
        IUploadRegion region = null;
        synchronized (this) {
            if (currentRegionIndex < regions.size()) {
                region = regions.get(currentRegionIndex);
            }
        }
        return region;
    }


    protected UploadRegionRequestMetrics getCurrentRegionRequestMetrics() {
        return currentRegionRequestMetrics;
    }

    // 一个上传流程可能会发起多个上传操作（如：上传多个分片），每个上传操作均是以一个Region的host做重试操作
    protected void addRegionRequestMetricsOfOneFlow(UploadRegionRequestMetrics metrics) {
        if (metrics == null) {
            return;
        }
        if (this.currentRegionRequestMetrics == null) {
            this.currentRegionRequestMetrics = metrics;
        } else {
            this.currentRegionRequestMetrics.addMetrics(metrics);
        }
    }

    protected static final String UploadUpTypeForm = "form";
    protected static final String UploadUpTypeResumableV1 = "resumable_v1";
    protected static final String UploadUpTypeResumableV2 = "resumable_v2";

    abstract String getUpType();

    protected interface UpTaskCompletionHandler {
        void complete(ResponseInfo responseInfo,
                      String key,
                      UploadTaskMetrics requestMetrics,
                      JSONObject response);
    }
}
