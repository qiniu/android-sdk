package com.qiniu.android.storage;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.request.RequestTransaction;
import com.qiniu.android.http.request.handler.RequestProgressHandler;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;

import org.json.JSONObject;

public class FormUpload extends BaseUpload {

    private boolean isAsyn = true;
    private double previousPercent;
    private RequestTransaction uploadTransaction;

    public FormUpload(byte[] data,
                      String key,
                      String fileName,
                      UpToken token,
                      UploadOptions option,
                      Configuration config,
                      UpTaskCompletionHandler completionHandler) {
        super(data, key, fileName, token, option, config, completionHandler);
    }

    @Override
    public void startToUpload() {

        uploadTransaction = new RequestTransaction(config, option, getTargetRegion(), getCurrentRegion(), key, token);

        RequestProgressHandler progressHandler = new RequestProgressHandler() {
            @Override
            public void progress(long totalBytesWritten, long totalBytesExpectedToWrite) {
                if (option.progressHandler != null){
                    double percent = (double)totalBytesWritten / (double)totalBytesExpectedToWrite;
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
            }
        };
        uploadTransaction.uploadFormData(data, fileName, isAsyn, progressHandler, new RequestTransaction.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                addRegionRequestMetricsOfOneFlow(requestMetrics);
                if (responseInfo.isOK()){
                    option.progressHandler.progress(key, 1.0);
                    completeAction(responseInfo, response);
                } else if (responseInfo.couldRetry() && config.allowBackupHost){
                    boolean isSwitched = switchRegionAndUpload();
                    if (!isSwitched){
                        completeAction(responseInfo, response);
                    }
                } else {
                    completeAction(responseInfo, response);
                }
            }
        });

    }
}
