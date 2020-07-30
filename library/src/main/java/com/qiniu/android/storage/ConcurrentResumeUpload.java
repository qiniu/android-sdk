package com.qiniu.android.storage;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.request.RequestTransaction;
import com.qiniu.android.http.request.UploadFileInfo;
import com.qiniu.android.http.request.IUploadRegion;
import com.qiniu.android.http.request.handler.RequestProgressHandler;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.utils.GroupTaskThread;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

class ConcurrentResumeUpload extends PartsUpload {

    private GroupTaskThread groupTaskThread;

    private double previousPercent;
    private ArrayList<RequestTransaction> uploadTransactions;

    private ResponseInfo uploadDataErrorResponseInfo;
    private JSONObject uploadDataErrorResponse;

    protected ConcurrentResumeUpload(File file,
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
    protected int prepareToUpload() {
        chunkSize = blockSize;

        return super.prepareToUpload();
    }

    @Override
    protected void startToUpload() {
        previousPercent = 0;
        uploadTransactions = new ArrayList<>();
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
                concurrentUploadRestData(new ResumeUploadCompleteHandler() {
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

    private void concurrentUploadRestData(final ResumeUploadCompleteHandler completeHandler){

        GroupTaskThread.GroupTaskCompleteHandler taskCompleteHandler = new GroupTaskThread.GroupTaskCompleteHandler() {
            @Override
            public void complete() {
                completeHandler.complete();
            }
        };

        groupTaskThread = new GroupTaskThread(taskCompleteHandler);
        for (int i = 0; i < config.concurrentTaskCount; i++) {
            groupTaskThread.addTask(new GroupTaskThread.GroupTask() {
                @Override
                public void run(final GroupTaskThread.GroupTask task) {
                    uploadRestData(new ResumeUploadCompleteHandler() {
                        @Override
                        public void complete() {
                            task.taskComplete();
                        }
                    });
                }
            });
        }

        groupTaskThread.start();
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

        synchronized (this) {
            final UploadFileInfo.UploadData data = uploadFileInfo.nextUploadData();

            RequestProgressHandler progressHandler = new RequestProgressHandler() {
                @Override
                public void progress(long totalBytesWritten, long totalBytesExpectedToWrite) {
                    data.progress = (double) totalBytesWritten / (double) totalBytesExpectedToWrite;
                    double percent = uploadFileInfo.progress();
                    if (percent > 0.95) {
                        percent = 0.95;
                    }
                    if (percent > previousPercent) {
                        previousPercent = percent;
                    } else {
                        percent = previousPercent;
                    }
                    option.progressHandler.progress(key, percent);
                }
            };
            if (data == null) {
                completeHandler.complete();
            } else {
                uploadDataFromServer(data, progressHandler, new UploadFileCompleteHandler() {
                    @Override
                    public void complete(ResponseInfo responseInfo, JSONObject response) {
                        if (!responseInfo.isOK()){
                            uploadDataErrorResponseInfo = responseInfo;
                            uploadDataErrorResponse = response;
                        } else {
                            uploadRestData(completeHandler);
                        }
                    }
                });
            }
        }
    }

    protected RequestTransaction createUploadRequestTransaction(){
        RequestTransaction transaction = new RequestTransaction(config, option, getTargetRegion(), getCurrentRegion(), key, token);
        uploadTransactions.add(transaction);
        return transaction;
    }

    private interface ResumeUploadCompleteHandler{
        void complete();
    }
}
