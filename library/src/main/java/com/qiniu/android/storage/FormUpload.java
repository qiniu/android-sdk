package com.qiniu.android.storage;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.request.RequestTransaction;
import com.qiniu.android.http.request.handler.RequestProgressHandler;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.utils.LogUtil;
import com.qiniu.android.utils.StringUtils;

import org.json.JSONObject;

class FormUpload extends BaseUpload {

    private boolean isAsync = true;
    private final UpProgress upProgress;
    private RequestTransaction uploadTransaction;

    protected FormUpload(byte[] data,
                         String key,
                         String fileName,
                         UpToken token,
                         UploadOptions option,
                         Configuration config,
                         UpTaskCompletionHandler completionHandler) {
        super(data, key, fileName, token, option, config, completionHandler);
        this.upProgress = new UpProgress(this.option.progressHandler);
    }

    @Override
    protected void startToUpload() {
        super.startToUpload();

        LogUtil.i("key:" + StringUtils.toNonnullString(key) + " form上传");

        uploadTransaction = new RequestTransaction(config, option, getTargetRegion(), getCurrentRegion(), key, token);

        RequestProgressHandler progressHandler = new RequestProgressHandler() {
            @Override
            public void progress(long totalBytesWritten, long totalBytesExpectedToWrite) {
                upProgress.progress(key, totalBytesWritten, totalBytesExpectedToWrite);
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

                upProgress.notifyDone(key, data.length);
                completeAction(responseInfo, response);
            }
        });
    }

    @Override
    String getUpType() {
        return UploadUpTypeForm;
    }
}
