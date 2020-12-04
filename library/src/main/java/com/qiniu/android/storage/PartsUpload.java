package com.qiniu.android.storage;

import com.qiniu.android.collect.ReportItem;
import com.qiniu.android.collect.UploadInfoReporter;
import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.http.request.RequestTransaction;
import com.qiniu.android.http.request.IUploadRegion;
import com.qiniu.android.http.request.handler.RequestProgressHandler;
import com.qiniu.android.utils.AsyncRun;
import com.qiniu.android.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Map;

class PartsUpload extends BaseUpload {

    PartsUploadPerformer uploadPerformer;

    private ResponseInfo uploadDataErrorResponseInfo;
    private JSONObject uploadDataErrorResponse;

    protected PartsUpload(File file,
                          String key,
                          UpToken token,
                          UploadOptions option,
                          Configuration config,
                          Recorder recorder,
                          String recorderKey,
                          UpTaskCompletionHandler completionHandler) {
        super(file, key, token, option, config, recorder, recorderKey, completionHandler);
    }

    @Override
    protected void initData() {
        super.initData();

        if (config != null && config.resumeUploadVersion == Configuration.RESUME_UPLOAD_VERSION_V1) {
            uploadPerformer = new PartsUploadPerformerV1(file, fileName, key, token, option, config, recorderKey);
        } else {
            uploadPerformer = new PartsUploadPerformerV2(file, fileName, key, token, option, config, recorderKey);
        }
    }

    boolean isAllUploaded() {
        if (uploadPerformer.fileInfo == null) {
            return false;
        } else {
            return uploadPerformer.fileInfo.isAllUploaded();
        }
    }

    private void setErrorResponse(ResponseInfo responseInfo, JSONObject response) {
        if (responseInfo == null) {
            return;
        }

        if (uploadDataErrorResponseInfo == null || (responseInfo.statusCode != ResponseInfo.NoUsableHostError)) {
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

        uploadDataErrorResponse = null;
        uploadDataErrorResponseInfo = null;

        if (uploadPerformer.currentRegion != null && uploadPerformer.currentRegion.isValid()) {
            insertRegionAtFirst(uploadPerformer.currentRegion);
        } else {
            uploadPerformer.switchRegion(getCurrentRegion());
        }

        if (file == null || !uploadPerformer.canReadFile()) {
            code = ResponseInfo.LocalIOError;
        }

        return code;
    }

    @Override
    protected boolean switchRegionAndUpload() {
        reportBlock();

        boolean isSwitched = super.switchRegionAndUpload();
        if (isSwitched) {
            uploadPerformer.switchRegion(getCurrentRegion());
        }
        return isSwitched;
    }

    @Override
    protected void startToUpload() {

        // 1. 启动upload
        serverInit(new UploadFileCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, JSONObject response) {

                if (!responseInfo.isOK()) {
                    completeAction(responseInfo, response);
                    return;
                }

                // 2. 上传数据
                uploadRestData(new UploadFileRestDataCompleteHandler() {
                    @Override
                    public void complete() {
                        if (!isAllUploaded()) {
                            if ((uploadDataErrorResponseInfo == null || uploadDataErrorResponseInfo.couldRetry()) && config.allowBackupHost) {
                                boolean isSwitched = switchRegionAndUpload();
                                if (!isSwitched) {
                                    completeAction(uploadDataErrorResponseInfo, uploadDataErrorResponse);
                                }
                            } else {
                                completeAction(uploadDataErrorResponseInfo, uploadDataErrorResponse);
                            }
                            return;
                        }

                        // 3. 组装文件
                        completeUpload(new UploadFileCompleteHandler() {
                            @Override
                            public void complete(ResponseInfo responseInfo, JSONObject response) {

                                if (responseInfo == null || !responseInfo.isOK()) {
                                    if ((responseInfo == null || responseInfo.couldRetry()) && config.allowBackupHost) {
                                        boolean isSwitched = switchRegionAndUpload();
                                        if (!isSwitched) {
                                            completeAction(responseInfo, response);
                                        }
                                    } else {
                                        completeAction(responseInfo, response);
                                    }
                                } else {
                                    AsyncRun.runInMain(new Runnable() {
                                        @Override
                                        public void run() {
                                            option.progressHandler.progress(key, 1.0);
                                        }
                                    });
                                    uploadPerformer.removeUploadInfoRecord();
                                    completeAction(responseInfo, response);
                                }
                            }
                        });
                    }
                });
            }
        });

    }

    protected void uploadRestData(final UploadFileRestDataCompleteHandler completeHandler) {
        performUploadRestData(completeHandler);
    }

    protected void performUploadRestData(final UploadFileRestDataCompleteHandler completeHandler) {
        if (isAllUploaded()){
            completeHandler.complete();
            return;
        }

        uploadNextDataCompleteHandler(new UploadFileDataCompleteHandler() {
            @Override
            public void complete(boolean stop, ResponseInfo responseInfo, JSONObject response) {
                if (stop || (responseInfo != null && !responseInfo.isOK())) {
                    setErrorResponse(responseInfo, response);
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

                addRegionRequestMetricsOfOneFlow(requestMetrics);
                completeHandler.complete(responseInfo, response);
            }
        });
    }

    protected void uploadNextDataCompleteHandler(final UploadFileDataCompleteHandler completeHandler) {

        uploadPerformer.uploadNextDataCompleteHandler(new PartsUploadPerformer.PartsUploadPerformerDataCompleteHandler() {
            @Override
            public void complete(boolean stop, ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                addRegionRequestMetricsOfOneFlow(requestMetrics);
                completeHandler.complete(stop, responseInfo, response);
            }
        });
    }

    protected void completeUpload(final UploadFileCompleteHandler completeHandler) {

        uploadPerformer.completeUpload(new PartsUploadPerformer.PartsUploadPerformerCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                addRegionRequestMetricsOfOneFlow(requestMetrics);
                completeHandler.complete(responseInfo, response);
            }
        });
    }

    @Override
    protected void completeAction(ResponseInfo responseInfo, JSONObject response) {
        reportBlock();
        uploadPerformer.closeFile();
        super.completeAction(responseInfo, response);
    }


    private void reportBlock() {

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
        item.setReport(currentZoneRegionId, ReportItem.BlockKeyTargetRegionId);
        item.setReport(targetZoneRegionId, ReportItem.BlockKeyCurrentRegionId);
        item.setReport(metrics.totalElapsedTime(), ReportItem.BlockKeyTotalElapsedTime);
        item.setReport(metrics.bytesSend(), ReportItem.BlockKeyBytesSent);
        item.setReport(uploadPerformer.recoveredFrom, ReportItem.BlockKeyRecoveredFrom);
        item.setReport(file.length(), ReportItem.BlockKeyFileSize);
        item.setReport(Utils.getCurrentProcessID(), ReportItem.BlockKeyPid);
        item.setReport(Utils.getCurrentThreadID(), ReportItem.BlockKeyTid);
        item.setReport(1, ReportItem.BlockKeyUpApiVersion);
        item.setReport(Utils.currentTimestamp(), ReportItem.BlockKeyClientTime);

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
