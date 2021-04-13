package com.qiniu.android.storage;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.http.request.RequestTransaction;
import com.qiniu.android.http.request.handler.RequestProgressHandler;
import com.qiniu.android.utils.Etag;
import com.qiniu.android.utils.LogUtil;
import com.qiniu.android.utils.StringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

class PartsUploadPerformerV2 extends PartsUploadPerformer {

    PartsUploadPerformerV2(File file,
                           String fileName,
                           String key,
                           UpToken token,
                           UploadOptions options,
                           Configuration config,
                           String recorderKey) {
        super(file, fileName, key, token, options, config, recorderKey);
    }

    @Override
    UploadFileInfo getFileFromJson(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        return UploadFileInfoPartV2.fileFromJson(jsonObject);
    }

    @Override
    UploadFileInfo getDefaultUploadFileInfo() {
        return new UploadFileInfoPartV2(file.length(), config.chunkSize, file.lastModified());
    }

    @Override
    void serverInit(final PartsUploadPerformerCompleteHandler completeHandler) {
        final UploadFileInfoPartV2 uploadFileInfo = (UploadFileInfoPartV2) fileInfo;
        if (uploadFileInfo != null && uploadFileInfo.isValid()) {
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
                    uploadFileInfo.uploadId = uploadId;
                    uploadFileInfo.expireAt = expireAt;
                    recordUploadInfo();
                }
                completeHandler.complete(responseInfo, requestMetrics, response);
            }
        });
    }

    @Override
    void uploadNextData(final PartsUploadPerformerDataCompleteHandler completeHandler) {
        UploadFileInfoPartV2 uploadFileInfo = (UploadFileInfoPartV2) fileInfo;

        UploadData data = null;
        synchronized (this) {
            data = uploadFileInfo.nextUploadData();
            if (data != null) {
                data.isUploading = true;
                data.isCompleted = false;
            }
        }

        if (data == null) {
            LogUtil.i("key:" + StringUtils.toNonnullString(key) + " no data left");

            ResponseInfo responseInfo = ResponseInfo.sdkInteriorError("no data left");
            completeHandler.complete(true, responseInfo, null, null);
            return;
        }

        data.data = getUploadData(data);
        final String etagA = Etag.data(data.data);
        if (data.data == null) {
            LogUtil.i("key:" + StringUtils.toNonnullString(key) + " get data error");

            data.isUploading = false;
            data.isCompleted = false;
            ResponseInfo responseInfo = ResponseInfo.localIOError("get data error");
            completeHandler.complete(true, responseInfo, null, responseInfo.response);
            return;
        }

        final UploadData uploadData = data;
        RequestProgressHandler progressHandler = new RequestProgressHandler() {
            @Override
            public void progress(long totalBytesWritten, long totalBytesExpectedToWrite) {
                uploadData.progress = (double) totalBytesWritten / (double) totalBytesExpectedToWrite;
                notifyProgress();
            }
        };

        final RequestTransaction transaction = createUploadRequestTransaction();
        transaction.uploadPart(true, uploadFileInfo.uploadId, data.index, data.data, progressHandler, new RequestTransaction.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {

                uploadData.data = null;
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
                boolean isEqual = etagA.equals(etag);
                System.out.println("===: isEqual:" + isEqual);
                if (responseInfo.isOK() && etag != null && md5 != null) {
                    uploadData.progress = 1;
                    uploadData.etag = etag;
                    uploadData.isUploading = false;
                    uploadData.isCompleted = true;
                    recordUploadInfo();
                    notifyProgress();
                } else {
                    uploadData.isUploading = false;
                    uploadData.isCompleted = false;
                }
                completeHandler.complete(false, responseInfo, requestMetrics, response);
            }
        });
    }

    @Override
    void completeUpload(final PartsUploadPerformerCompleteHandler completeHandler) {
        final UploadFileInfoPartV2 uploadFileInfo = (UploadFileInfoPartV2) fileInfo;

        List<Map<String, Object>> partInfoArray = uploadFileInfo.getPartInfoArray();
        final RequestTransaction transaction = createUploadRequestTransaction();

        transaction.completeParts(true, fileName, uploadFileInfo.uploadId, partInfoArray, new RequestTransaction.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {

                destroyUploadRequestTransaction(transaction);
                completeHandler.complete(responseInfo, requestMetrics, response);
            }
        });
    }

    private synchronized byte[] getUploadData(UploadData data) {
        if (randomAccessFile == null || data == null) {
            return null;
        }

        int readSize = 0;
        byte[] uploadData = new byte[(int) data.size];
        try {
            randomAccessFile.seek(data.offset);
            while (readSize < data.size) {
                int ret = randomAccessFile.read(uploadData, readSize, (int) (data.size - readSize));
                if (ret < 0) {
                    break;
                }
                readSize += ret;
            }
        } catch (IOException e) {
            uploadData = null;
        }
        return uploadData;
    }
}
