package com.qiniu.android.storage;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.request.RequestTransaction;
import com.qiniu.android.http.request.handler.RequestProgressHandler;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.utils.AsyncRun;
import com.qiniu.android.utils.LogUtil;
import com.qiniu.android.utils.StringUtils;

import org.json.JSONObject;

class FormUpload extends BaseUpload {

    private boolean isAsync = true;
    private double previousPercent;
    private RequestTransaction uploadTransaction;

    protected FormUpload(byte[] data,
                         String key,
                         String fileName,
                         UpToken token,
                         UploadOptions option,
                         Configuration config,
                         UpTaskCompletionHandler completionHandler) {
        super(data, key, fileName, token, option, config, completionHandler);
    }

    @Override
    protected void startToUpload() {

        LogUtil.i("key:" + StringUtils.nullToEmpty(key) + " form上传");

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
        uploadTransaction.uploadFormData(data, fileName, isAsync, progressHandler, new RequestTransaction.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                addRegionRequestMetricsOfOneFlow(requestMetrics);

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
}
