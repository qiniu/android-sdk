package com.qiniu.android.storage;

import com.qiniu.android.collect.ReportItem;
import com.qiniu.android.collect.UploadInfoReporter;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.utils.AsyncRun;
import com.qiniu.android.utils.LogUtil;
import com.qiniu.android.utils.StringUtils;
import com.qiniu.android.utils.Utils;

import org.json.JSONObject;

import java.io.File;

class PartsUpload extends BaseUpload {

    PartsUploadPerformer uploadPerformer;

    private ResponseInfo uploadDataErrorResponseInfo;
    private JSONObject uploadDataErrorResponse;

    protected PartsUpload(UploadSource source,
                          String key,
                          UpToken token,
                          UploadOptions option,
                          Configuration config,
                          Recorder recorder,
                          String recorderKey,
                          UpTaskCompletionHandler completionHandler) {
        super(source, key, token, option, config, recorder, recorderKey, completionHandler);
    }

    @Override
    protected void initData() {
        super.initData();

        if (config != null && config.resumeUploadVersion == Configuration.RESUME_UPLOAD_VERSION_V1) {
            LogUtil.i("key:" + StringUtils.toNonnullString(key) + " 分片V1");
            uploadPerformer = new PartsUploadPerformerV1(uploadSource, fileName, key, token, option, config, recorderKey);
        } else {
            LogUtil.i("key:" + StringUtils.toNonnullString(key) + " 分片V2");
            uploadPerformer = new PartsUploadPerformerV2(uploadSource, fileName, key, token, option, config, recorderKey);
        }
    }

    boolean isAllUploaded() {
        if (uploadPerformer.uploadInfo == null) {
            return false;
        } else {
            return uploadPerformer.uploadInfo.isAllUploaded();
        }
    }

    private void setErrorResponse(ResponseInfo responseInfo, JSONObject response) {
        if (responseInfo == null) {
            return;
        }

        if (uploadDataErrorResponseInfo == null || responseInfo.statusCode != ResponseInfo.SDKInteriorError) {
            uploadDataErrorResponseInfo = responseInfo;
            if (response == null) {
                uploadDataErrorResponse = responseInfo.response;
            } else {
                uploadDataErrorResponse = response;
            }
        }
    }

    @Override
    protected int prepareToUpload() {
        int code = super.prepareToUpload();
        if (code != 0) {
            return code;
        }

        if (uploadPerformer.currentRegion != null && uploadPerformer.currentRegion.isValid()) {
            insertRegionAtFirst(uploadPerformer.currentRegion);
            LogUtil.i("key:" + StringUtils.toNonnullString(key) + " 使用缓存region");
        } else {
            uploadPerformer.switchRegion(getCurrentRegion());
        }

        if (uploadPerformer != null && uploadPerformer.currentRegion != null && uploadPerformer.currentRegion.getZoneInfo() != null) {
            LogUtil.i("key:" + StringUtils.toNonnullString(key) + " region:" + StringUtils.toNonnullString(uploadPerformer.currentRegion.getZoneInfo().regionId));
        }

        if (!uploadPerformer.canReadFile()) {
            code = ResponseInfo.LocalIOError;
        }

        return code;
    }

    @Override
    protected boolean switchRegion() {
        // 重新加载资源，如果加载失败，不可切换 region
        if (!uploadSource.couldReloadInfo() || !uploadSource.reloadInfo()) {
            return false;
        }

        boolean isSuccess = super.switchRegion();
        if (isSuccess) {
            uploadPerformer.switchRegion(getCurrentRegion());
            if (uploadPerformer != null && uploadPerformer.currentRegion != null && uploadPerformer.currentRegion.getZoneInfo() != null) {
                LogUtil.i("key:" + StringUtils.toNonnullString(key) + " region:" + StringUtils.toNonnullString(uploadPerformer.currentRegion.getZoneInfo().regionId));
            }
        }
        return isSuccess;
    }

    @Override
    protected boolean switchRegionAndUpload() {
        reportBlock();
        return super.switchRegionAndUpload();
    }

    @Override
    protected void startToUpload() {
        uploadDataErrorResponse = null;
        uploadDataErrorResponseInfo = null;

        LogUtil.i("key:" + StringUtils.toNonnullString(key) + " serverInit");

        // 1. 启动upload
        serverInit(new UploadFileCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, JSONObject response) {

                if (!responseInfo.isOK()) {
                    if (!switchRegionAndUploadIfNeededWithErrorResponse(responseInfo)) {
                        completeAction(responseInfo, response);
                    }
                    return;
                }

                LogUtil.i("key:" + StringUtils.toNonnullString(key) + " uploadRestData");

                // 2. 上传数据
                uploadRestData(new UploadFileRestDataCompleteHandler() {
                    @Override
                    public void complete() {
                        if (!isAllUploaded()) {
                            if (!switchRegionAndUploadIfNeededWithErrorResponse(uploadDataErrorResponseInfo)) {
                                completeAction(uploadDataErrorResponseInfo, uploadDataErrorResponse);
                            }
                            return;
                        }

                        LogUtil.i("key:" + StringUtils.toNonnullString(key) + " completeUpload");

                        // 3. 组装文件
                        completeUpload(new UploadFileCompleteHandler() {
                            @Override
                            public void complete(ResponseInfo responseInfo, JSONObject response) {

                                if (!responseInfo.isOK()) {
                                    if (!switchRegionAndUploadIfNeededWithErrorResponse(responseInfo)) {
                                        completeAction(responseInfo, response);
                                    }
                                    return;
                                }

                                AsyncRun.runInMain(new Runnable() {
                                    @Override
                                    public void run() {
                                        option.progressHandler.progress(key, 1.0);
                                    }
                                });
                                completeAction(responseInfo, response);
                            }
                        });
                    }
                });
            }
        });

    }

    protected void uploadRestData(final UploadFileRestDataCompleteHandler completeHandler) {
        LogUtil.i("key:" + StringUtils.toNonnullString(key) + " 串行分片");
        performUploadRestData(completeHandler);
    }

    protected void performUploadRestData(final UploadFileRestDataCompleteHandler completeHandler) {
        if (isAllUploaded()) {
            completeHandler.complete();
            return;
        }

        uploadNextData(new UploadFileDataCompleteHandler() {
            @Override
            public void complete(boolean stop, ResponseInfo responseInfo, JSONObject response) {
                if (stop || (responseInfo != null && !responseInfo.isOK())) {
                    completeHandler.complete();
                } else {
                    performUploadRestData(completeHandler);
                }
            }
        });
    }


    protected void serverInit(final UploadFileCompleteHandler completeHandler) {

        uploadPerformer.serverInit(new PartsUploadPerformer.PartsUploadPerformerCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                if (responseInfo != null && !responseInfo.isOK()) {
                    setErrorResponse(responseInfo, response);
                }
                addRegionRequestMetricsOfOneFlow(requestMetrics);
                completeHandler.complete(responseInfo, response);
            }
        });
    }

    protected void uploadNextData(final UploadFileDataCompleteHandler completeHandler) {

        uploadPerformer.uploadNextData(new PartsUploadPerformer.PartsUploadPerformerDataCompleteHandler() {
            @Override
            public void complete(boolean stop, ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                if (responseInfo != null && !responseInfo.isOK()) {
                    setErrorResponse(responseInfo, response);
                }
                addRegionRequestMetricsOfOneFlow(requestMetrics);
                completeHandler.complete(stop, responseInfo, response);
            }
        });
    }

    protected void completeUpload(final UploadFileCompleteHandler completeHandler) {

        uploadPerformer.completeUpload(new PartsUploadPerformer.PartsUploadPerformerCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                if (responseInfo != null && !responseInfo.isOK()) {
                    setErrorResponse(responseInfo, response);
                }
                addRegionRequestMetricsOfOneFlow(requestMetrics);
                completeHandler.complete(responseInfo, response);
            }
        });
    }

    @Override
    protected void completeAction(ResponseInfo responseInfo, JSONObject response) {
        reportBlock();
        uploadPerformer.closeFile();
        if (shouldRemoveUploadInfoRecord(responseInfo)) {
            uploadPerformer.removeUploadInfoRecord();
        }
        super.completeAction(responseInfo, response);
    }

    private boolean shouldRemoveUploadInfoRecord(ResponseInfo responseInfo) {
        return responseInfo != null && (responseInfo.isOK() || responseInfo.statusCode == 612 || responseInfo.statusCode == 614 || responseInfo.statusCode == 701);
    }

    private void reportBlock() {

        if (token == null || !token.isValid()) {
            return;
        }

        UploadRegionRequestMetrics metrics = getCurrentRegionRequestMetrics();
        if (metrics == null) {
            metrics = new UploadRegionRequestMetrics(null);
        }

        String currentZoneRegionId = null;
        if (getCurrentRegion() != null && getCurrentRegion().getZoneInfo() != null && getCurrentRegion().getZoneInfo().regionId != null) {
            currentZoneRegionId = getCurrentRegion().getZoneInfo().regionId;
        }
        String targetZoneRegionId = null;
        if (getTargetRegion() != null && getTargetRegion().getZoneInfo() != null && getTargetRegion().getZoneInfo().regionId != null) {
            targetZoneRegionId = getTargetRegion().getZoneInfo().regionId;
        }

        ReportItem item = new ReportItem();
        item.setReport(ReportItem.LogTypeBlock, ReportItem.BlockKeyLogType);
        item.setReport((Utils.currentTimestamp() / 1000), ReportItem.BlockKeyUpTime);
        item.setReport(key, ReportItem.QualityKeyTargetKey);
        item.setReport(token.bucket, ReportItem.QualityKeyTargetBucket);
        item.setReport(currentZoneRegionId, ReportItem.BlockKeyTargetRegionId);
        item.setReport(targetZoneRegionId, ReportItem.BlockKeyCurrentRegionId);
        item.setReport(metrics.totalElapsedTime(), ReportItem.BlockKeyTotalElapsedTime);
        item.setReport(metrics.bytesSend(), ReportItem.BlockKeyBytesSent);
        item.setReport(uploadPerformer.recoveredFrom, ReportItem.BlockKeyRecoveredFrom);
        item.setReport(uploadSource.getSize(), ReportItem.BlockKeyFileSize);
        item.setReport(Utils.getCurrentProcessID(), ReportItem.BlockKeyPid);
        item.setReport(Utils.getCurrentThreadID(), ReportItem.BlockKeyTid);

        if (config != null && config.resumeUploadVersion == Configuration.RESUME_UPLOAD_VERSION_V1) {
            item.setReport(1, ReportItem.BlockKeyUpApiVersion);
        } else {
            item.setReport(2, ReportItem.BlockKeyUpApiVersion);
        }

        item.setReport(Utils.currentTimestamp(), ReportItem.BlockKeyClientTime);

        item.setReport(Utils.systemName(), ReportItem.BlockKeyOsName);
        item.setReport(Utils.systemVersion(), ReportItem.BlockKeyOsVersion);
        item.setReport(Utils.sdkLanguage(), ReportItem.BlockKeySDKName);
        item.setReport(Utils.sdkVerion(), ReportItem.BlockKeySDKVersion);

        UploadInfoReporter.getInstance().report(item, token.token);
    }

    protected interface UploadFileRestDataCompleteHandler {
        void complete();
    }

    protected interface UploadFileCompleteHandler {
        void complete(ResponseInfo responseInfo, JSONObject response);
    }

    protected interface UploadFileDataCompleteHandler {
        void complete(boolean stop, ResponseInfo responseInfo, JSONObject response);
    }
}
