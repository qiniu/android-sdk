package com.qiniu.android.storage;

import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.http.request.IUploadRegion;
import com.qiniu.android.http.request.RequestTransaction;
import com.qiniu.android.http.serverRegion.UploadDomainRegion;
import com.qiniu.android.utils.LogUtil;
import com.qiniu.android.utils.StringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

abstract class PartsUploadPerformer {
    private static final String kRecordFileInfoKey = "recordFileInfo";
    private static final String kRecordZoneInfoKey = "recordZoneInfo";

    final String key;
    final String fileName;
    final UploadSource uploadSource;
    private final UpProgress upProgress;

    final UpToken token;
    final UploadOptions options;
    final Configuration config;
    final Recorder recorder;
    final String recorderKey;

    private IUploadRegion targetRegion;
    protected IUploadRegion currentRegion;

    Long recoveredFrom;
    UploadInfo uploadInfo;
    List<RequestTransaction> uploadTransactions;

    PartsUploadPerformer(UploadSource uploadSource,
                         String fileName,
                         String key,
                         UpToken token,
                         UploadOptions options,
                         Configuration config,
                         String recorderKey) {
        this.uploadSource = uploadSource;
        this.key = key;
        this.fileName = fileName;
        this.token = token;
        this.options = options;
        this.config = config;
        this.recorder = config.recorder;
        this.recorderKey = recorderKey;
        this.upProgress = new UpProgress(this.options.progressHandler);

        this.initData();
    }

    void initData() {
        uploadTransactions = new ArrayList<>();
        uploadInfo = getDefaultUploadInfo();
        recoverUploadInfoFromRecord();
    }

    boolean canReadFile() {
        return uploadInfo != null && uploadInfo.hasValidResource();
    }

    boolean couldReloadInfo() {
        return uploadInfo.couldReloadSource();
    }

    boolean reloadInfo() {
        if (uploadInfo == null) {
            return false;
        }

        recoveredFrom = null;
        uploadInfo.clearUploadState();
        return uploadInfo.reloadSource();
    }

    void closeFile() {
        if (uploadInfo != null) {
            uploadInfo.close();
        }
    }

    void switchRegion(IUploadRegion region) {
        currentRegion = region;
        if (targetRegion == null) {
            targetRegion = region;
        }
    }

    void notifyProgress(Boolean isCompleted) {
        if (uploadInfo == null) {
            return;
        }

        if (isCompleted) {
            upProgress.notifyDone(key, uploadInfo.getSourceSize());
        } else {
            upProgress.progress(key, uploadInfo.uploadSize(), uploadInfo.getSourceSize());
        }
    }

    void recordUploadInfo() {

        String key = recorderKey;
        if (recorder == null || key == null || key.length() == 0) {
            return;
        }

        synchronized (this) {
            JSONObject zoneInfoJson = null;
            JSONObject fileInfoJson = null;
            if (currentRegion != null && currentRegion.getZoneInfo() != null) {
                zoneInfoJson = currentRegion.getZoneInfo().detailInfo;
            }
            if (uploadInfo != null) {
                fileInfoJson = uploadInfo.toJsonObject();
            }
            if (zoneInfoJson != null && fileInfoJson != null) {
                JSONObject info = new JSONObject();
                try {
                    info.put(kRecordZoneInfoKey, zoneInfoJson);
                    info.put(kRecordFileInfoKey, fileInfoJson);
                } catch (JSONException ignored) {
                }
                recorder.set(key, info.toString().getBytes());
            }
        }
        LogUtil.i("key:" + StringUtils.toNonnullString(key) +
                " recorderKey:" + StringUtils.toNonnullString(recorderKey) +
                " recordUploadInfo");
    }

    void removeUploadInfoRecord() {
        recoveredFrom = null;
        if (uploadInfo != null) {
            uploadInfo.clearUploadState();
        }
        if (recorder != null && recorderKey != null) {
            recorder.del(recorderKey);
        }
        LogUtil.i("key:" + StringUtils.toNonnullString(key) +
                " recorderKey:" + StringUtils.toNonnullString(recorderKey) +
                " removeUploadInfoRecord");
    }

    void recoverUploadInfoFromRecord() {
        LogUtil.i("key:" + StringUtils.toNonnullString(key) +
                " recorderKey:" + StringUtils.toNonnullString(recorderKey) +
                " recorder:" + StringUtils.toNonnullString(recorder) +
                " recoverUploadInfoFromRecord");

        String key = recorderKey;
        if (recorder == null || key == null || key.length() == 0 || uploadSource == null) {
            return;
        }

        byte[] data = recorder.get(key);
        if (data == null) {
            LogUtil.i("key:" + StringUtils.toNonnullString(key) +
                    " recorderKey:" + StringUtils.toNonnullString(recorderKey) +
                    " recoverUploadInfoFromRecord data:null");
            return;
        }

        try {
            JSONObject info = new JSONObject(new String(data));
            ZoneInfo zoneInfo = ZoneInfo.buildFromJson(info.getJSONObject(kRecordZoneInfoKey));
            UploadInfo recoverUploadInfo = getUploadInfoFromJson(uploadSource, info.getJSONObject(kRecordFileInfoKey));
            if (zoneInfo != null && recoverUploadInfo != null && recoverUploadInfo.isValid() && uploadInfo.isSameUploadInfo(recoverUploadInfo)) {

                LogUtil.i("key:" + StringUtils.toNonnullString(key) +
                        " recorderKey:" + StringUtils.toNonnullString(recorderKey) +
                        " recoverUploadInfoFromRecord valid");

                recoverUploadInfo.checkInfoStateAndUpdate();
                uploadInfo = recoverUploadInfo;
                UploadDomainRegion region = new UploadDomainRegion();
                region.setupRegionData(zoneInfo);
                currentRegion = region;
                targetRegion = region;
                recoveredFrom = recoverUploadInfo.uploadSize();
            } else {
                LogUtil.i("key:" + StringUtils.toNonnullString(key) +
                        " recorderKey:" + StringUtils.toNonnullString(recorderKey) +
                        " recoverUploadInfoFromRecord invalid");

                recorder.del(key);
                currentRegion = null;
                targetRegion = null;
                recoveredFrom = null;
            }
        } catch (Exception e) {
            LogUtil.i("key:" + StringUtils.toNonnullString(key) +
                    " recorderKey:" + StringUtils.toNonnullString(recorderKey) +
                    " recoverUploadInfoFromRecord json:error");

            recorder.del(key);
            currentRegion = null;
            targetRegion = null;
            recoveredFrom = null;
        }
    }

    RequestTransaction createUploadRequestTransaction() {
        final RequestTransaction transaction = new RequestTransaction(config, options, targetRegion, currentRegion, key, token);
        synchronized (this) {
            if (uploadTransactions != null) {
                uploadTransactions.add(transaction);
            }
        }
        return transaction;
    }

    void destroyUploadRequestTransaction(RequestTransaction transaction) {
        if (transaction != null) {
            synchronized (this) {
                if (uploadTransactions != null) {
                    uploadTransactions.remove(transaction);
                }
            }
        }
    }

    abstract UploadInfo getDefaultUploadInfo();

    abstract UploadInfo getUploadInfoFromJson(UploadSource source, JSONObject jsonObject);

    abstract void serverInit(PartsUploadPerformerCompleteHandler completeHandler);

    abstract void uploadNextData(PartsUploadPerformerDataCompleteHandler completeHandler);

    abstract void completeUpload(PartsUploadPerformerCompleteHandler completeHandler);

    interface PartsUploadPerformerCompleteHandler {
        void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response);
    }

    interface PartsUploadPerformerDataCompleteHandler {
        void complete(boolean stop, ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response);
    }
}
