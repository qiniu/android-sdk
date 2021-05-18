package com.qiniu.android.storage;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.http.request.RequestTransaction;
import com.qiniu.android.http.request.handler.RequestProgressHandler;
import com.qiniu.android.utils.LogUtil;
import com.qiniu.android.utils.StringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

class PartsUploadPerformerV2 extends PartsUploadPerformer {

    PartsUploadPerformerV2(UploadSource uploadSource,
                           String fileName,
                           String key,
                           UpToken token,
                           UploadOptions options,
                           Configuration config,
                           String recorderKey) {
        super(uploadSource, fileName, key, token, options, config, recorderKey);
    }

    @Override
    UploadInfo getUploadInfoFromJson(UploadSource source, JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        return UploadInfoV2.infoFromJson(source, jsonObject);
    }

    @Override
    UploadInfo getDefaultUploadInfo() {
        return new UploadInfoV2(uploadSource, config);
    }

    @Override
    void serverInit(final PartsUploadPerformerCompleteHandler completeHandler) {
        final UploadInfoV2 info = (UploadInfoV2) uploadInfo;
        if (info != null && info.isValid()) {
            LogUtil.i("key:" + StringUtils.toNonnullString(key) + " serverInit success");
            ResponseInfo responseInfo = ResponseInfo.successResponse();
            completeHandler.complete(responseInfo, null, null);
            return;
        }

        final RequestTransaction transaction = createUploadRequestTransaction();
        transaction.initPart(true, new RequestTransaction.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {

                destroyUploadRequestTransaction(transaction);

                String uploadId = null;
                Long expireAt = null;
                if (response != null) {
                    try {
                        uploadId = response.getString("uploadId");
                        expireAt = response.getLong("expireAt");
                    } catch (JSONException e) {
                    }
                }
                if (responseInfo.isOK() && uploadId != null && expireAt != null) {
                    info.uploadId = uploadId;
                    info.expireAt = expireAt;
                    recordUploadInfo();
                }
                completeHandler.complete(responseInfo, requestMetrics, response);
            }
        });
    }

    @Override
    void uploadNextData(final PartsUploadPerformerDataCompleteHandler completeHandler) {
        final UploadInfoV2 info = (UploadInfoV2) uploadInfo;

        UploadData data = null;
        synchronized (this) {
            try {
                data = info.nextUploadData();
                if (data != null) {
                    data.updateState(UploadData.State.Uploading);
                }
            } catch (Exception e) {
                // 此处可能无法恢复
                LogUtil.i("key:" + StringUtils.toNonnullString(key) + " " + e.getMessage());

                ResponseInfo responseInfo = ResponseInfo.localIOError(e.getMessage());
                completeHandler.complete(true, responseInfo, null, responseInfo.response);
                return;
            }
        }

        if (data == null) {
            LogUtil.i("key:" + StringUtils.toNonnullString(key) + " no data left");

            ResponseInfo responseInfo = null;
            if (uploadInfo.getSourceSize() == 0) {
                responseInfo = ResponseInfo.zeroSize("file is empty");
            } else {
                responseInfo = ResponseInfo.sdkInteriorError("no chunk left");
            }
            completeHandler.complete(true, responseInfo, null, null);
            return;
        }

        final UploadData uploadData = data;
        RequestProgressHandler progressHandler = new RequestProgressHandler() {
            @Override
            public void progress(long totalBytesWritten, long totalBytesExpectedToWrite) {
                uploadData.setUploadSize(totalBytesWritten);
                notifyProgress(false);
            }
        };

        final RequestTransaction transaction = createUploadRequestTransaction();
        transaction.uploadPart(true, info.uploadId, info.getPartIndexOfData(data), data.data, progressHandler, new RequestTransaction.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {

                destroyUploadRequestTransaction(transaction);

                String etag = null;
                String md5 = null;
                if (response != null) {
                    try {
                        etag = response.getString("etag");
                        md5 = response.getString("md5");
                    } catch (JSONException e) {
                    }
                }
                if (responseInfo.isOK() && etag != null && md5 != null) {
                    uploadData.etag = etag;
                    uploadData.updateState(UploadData.State.Complete);
                    recordUploadInfo();
                    notifyProgress(false);
                } else {
                    uploadData.updateState(UploadData.State.WaitToUpload);
                }
                completeHandler.complete(false, responseInfo, requestMetrics, response);
            }
        });
    }

    @Override
    void completeUpload(final PartsUploadPerformerCompleteHandler completeHandler) {
        final UploadInfoV2 info = (UploadInfoV2) uploadInfo;

        List<Map<String, Object>> partInfoArray = info.getPartInfoArray();
        final RequestTransaction transaction = createUploadRequestTransaction();

        transaction.completeParts(true, fileName, info.uploadId, partInfoArray, new RequestTransaction.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                if (responseInfo.isOK()) {
                    notifyProgress(true);
                }

                destroyUploadRequestTransaction(transaction);
                completeHandler.complete(responseInfo, requestMetrics, response);
            }
        });
    }
}
