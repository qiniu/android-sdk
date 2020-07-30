package com.qiniu.android.storage;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.request.RequestTransaction;
import com.qiniu.android.http.request.UploadFileInfo;
import com.qiniu.android.http.request.IUploadRegion;
import com.qiniu.android.http.request.handler.RequestProgressHandler;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

class ResumeUpload extends PartsUpload {

    private double previousPercent;
    private RequestTransaction uploadTransaction;

    private ResponseInfo uploadDataErrorResponseInfo;
    private JSONObject uploadDataErrorResponse;

    protected ResumeUpload(File file,
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
    protected void startToUpload() {
        previousPercent = 0;
        uploadDataErrorResponseInfo = null;
        uploadDataErrorResponse = null;

        // 1. 启动upload
        initPartFromServer(new UploadFileCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, JSONObject response) {

                if (responseInfo == null || !responseInfo.isOK()) {
                    boolean isSwitched = switchRegionAndUpload();
                    if (!isSwitched){
                        completeAction(uploadDataErrorResponseInfo, uploadDataErrorResponse);
                    }
                    return;
                }

                UploadFileInfo fileInfo = getUploadFileInfo();
                if (!responseInfo.isOK() || fileInfo.uploadId == null || fileInfo.uploadId.length() == 0){
                    completeAction(responseInfo, response);
                    return;
                }

                // 2. 上传数据
                uploadRestData(new ResumeUploadCompleteHandler() {
                    @Override
                    public void complete() {

                        UploadFileInfo uploadFileInfo = getUploadFileInfo();
                        if (!uploadFileInfo.isAllUploaded() || uploadDataErrorResponseInfo != null){
                            if (uploadDataErrorResponseInfo.couldRetry() && config.allowBackupHost) {
                                boolean isSwitched = switchRegionAndUpload();
                                if (!isSwitched){
                                    completeAction(uploadDataErrorResponseInfo, uploadDataErrorResponse);
                                }
                            } else {
                                completeAction(uploadDataErrorResponseInfo, uploadDataErrorResponse);
                            }
                            return;
                        }

                        // 3. 组装文件
                        completePartsFromServer(new UploadFileCompleteHandler() {
                            @Override
                            public void complete(ResponseInfo responseInfo, JSONObject response) {
                                if (responseInfo == null || !responseInfo.isOK()){
                                    boolean isSwitched = switchRegionAndUpload();
                                    if (!isSwitched){
                                        completeAction(responseInfo, response);
                                    }
                                } else {
                                    option.progressHandler.progress(key, 1.0);
                                    removeUploadInfoRecord();
                                    completeAction(responseInfo, response);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    private void uploadRestData(final ResumeUploadCompleteHandler completeHandler){
        final UploadFileInfo uploadFileInfo = getUploadFileInfo();
        if (uploadFileInfo == null){
            if (uploadDataErrorResponseInfo == null){
                uploadDataErrorResponseInfo = ResponseInfo.invalidArgument("file error");
                uploadDataErrorResponse = uploadDataErrorResponseInfo.response;
            }
            completeHandler.complete();
            return;
        }

        IUploadRegion currentRegion = getCurrentRegion();
        if (currentRegion == null){
            if (uploadDataErrorResponseInfo == null){
                uploadDataErrorResponseInfo = ResponseInfo.invalidArgument("server error");
                uploadDataErrorResponse = uploadDataErrorResponseInfo.response;
            }
            completeHandler.complete();
            return;
        }

        final UploadFileInfo.UploadData data = uploadFileInfo.nextUploadData();

        RequestProgressHandler progressHandler = new RequestProgressHandler() {
            @Override
            public void progress(long totalBytesWritten, long totalBytesExpectedToWrite) {
                data.progress = (double)totalBytesWritten / (double)totalBytesExpectedToWrite;
                double percent = uploadFileInfo.progress();
                if (percent > 0.95){
                    percent = 0.95;
                }
                if (percent > previousPercent){
                    previousPercent = percent;
                } else {
                    percent = previousPercent;
                }
                option.progressHandler.progress(key, percent);
            }
        };
        if (data == null){
            completeHandler.complete();
        } else {
            uploadDataFromServer(data, progressHandler, new UploadFileCompleteHandler() {
                @Override
                public void complete(ResponseInfo responseInfo, JSONObject response) {
                    if (!responseInfo.isOK()) {
                        uploadDataErrorResponseInfo = responseInfo;
                        uploadDataErrorResponse = response;
                        completeHandler.complete();
                    } else {
                        uploadRestData(completeHandler);
                    }
                }
            });
        }
    }

    @Override
    protected RequestTransaction createUploadRequestTransaction() {
        uploadTransaction = new RequestTransaction(config, option, getTargetRegion(), getCurrentRegion(), key, token);
        return uploadTransaction;
    }

    private interface ResumeUploadCompleteHandler{
        void complete();
    }

}
