package com.qiniu.android.storage;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.http.request.RequestTransaction;
import com.qiniu.android.http.request.handler.RequestProgressHandler;
import com.qiniu.android.utils.LogUtil;
import com.qiniu.android.utils.StringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

class PartsUploadPerformerV1 extends PartsUploadPerformer {

    PartsUploadPerformerV1(UploadSource uploadSource,
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
        return UploadInfoV1.infoFromJson(source, jsonObject);
    }

    @Override
    UploadInfo getDefaultUploadInfo() {
        return new UploadInfoV1(uploadSource, config);
    }

    @Override
    void serverInit(PartsUploadPerformerCompleteHandler completeHandler) {
        ResponseInfo responseInfo = ResponseInfo.successResponse();
        completeHandler.complete(responseInfo, null, null);
    }

    @Override
    void uploadNextData(final PartsUploadPerformerDataCompleteHandler completeHandler) {
        UploadInfoV1 info = (UploadInfoV1) uploadInfo;
        UploadBlock block = null;
        UploadData chunk = null;

        synchronized (this) {
            try {
                block = info.nextUploadBlock();
                chunk = info.nextUploadData(block);
                if (chunk != null) {
                    chunk.updateState(UploadData.State.Uploading);
                }
            } catch (Exception e) {
                // 此处可能导致后面无法恢复
                LogUtil.i("key:" + StringUtils.toNonnullString(key) + e.getMessage());

                ResponseInfo responseInfo = ResponseInfo.localIOError(e.getMessage());
                completeHandler.complete(true, responseInfo, null, null);
                return;
            }
        }

        if (block == null || chunk == null) {
            LogUtil.i("key:" + StringUtils.toNonnullString(key) + " no chunk left");
            ResponseInfo responseInfo = null;
            if (uploadInfo.getSourceSize() == 0) {
                responseInfo = ResponseInfo.zeroSize("file is empty");
            } else {
                responseInfo = ResponseInfo.sdkInteriorError("no chunk left");
            }
            completeHandler.complete(true, responseInfo, null, null);
            return;
        }

        if (chunk.data == null) {
            LogUtil.i("key:" + StringUtils.toNonnullString(key) + " get chunk null");
            ResponseInfo responseInfo = ResponseInfo.invalidArgument("chunk data is null");
            completeHandler.complete(true, responseInfo, null, null);
            return;
        }

        final UploadBlock uploadBlock = block;
        final UploadData uploadChunk = chunk;
        RequestProgressHandler progressHandler = new RequestProgressHandler() {
            @Override
            public void progress(long totalBytesWritten, long totalBytesExpectedToWrite) {
                uploadChunk.setUploadSize(totalBytesWritten);
                notifyProgress(false);
            }
        };
        PartsUploadPerformerCompleteHandler completeHandlerP = new PartsUploadPerformerCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {

                String ctx = null;
                Long expiredAt = null;
                if (response != null) {
                    try {
                        ctx = response.getString("ctx");
                        expiredAt = response.getLong("expired_at");
                    } catch (JSONException e) {
                    }
                }
                if (responseInfo.isOK() && ctx != null && expiredAt != null) {
                    uploadBlock.ctx = ctx;
                    uploadBlock.expireAt = expiredAt;
                    uploadChunk.updateState(UploadData.State.Complete);
                    recordUploadInfo();
                    notifyProgress(false);
                } else {
                    uploadChunk.updateState(UploadData.State.WaitToUpload);
                }
                completeHandler.complete(false, responseInfo, requestMetrics, response);
            }
        };

        if (info.isFirstData(uploadChunk)) {
            LogUtil.i("key:" + StringUtils.toNonnullString(key) + " makeBlock");
            makeBlock(uploadBlock, uploadChunk, progressHandler, completeHandlerP);
        } else {
            LogUtil.i("key:" + StringUtils.toNonnullString(key) + " uploadChunk");
            uploadChunk(uploadBlock, uploadChunk, progressHandler, completeHandlerP);
        }
    }

    @Override
    void completeUpload(final PartsUploadPerformerCompleteHandler completeHandler) {
        UploadInfoV1 info = (UploadInfoV1) uploadInfo;

        String[] contexts = null;
        ArrayList<String> contextsList = info.allBlocksContexts();

        if (contextsList != null && contextsList.size() > 0) {
            contexts = contextsList.toArray(new String[contextsList.size()]);
        }

        final RequestTransaction transaction = createUploadRequestTransaction();
        transaction.makeFile(info.getSourceSize(), fileName, contexts, true, new RequestTransaction.RequestCompleteHandler() {

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

    private void makeBlock(final UploadBlock block,
                           final UploadData chunk,
                           final RequestProgressHandler progressHandler,
                           final PartsUploadPerformerCompleteHandler completeHandler) {

        final RequestTransaction transaction = createUploadRequestTransaction();
        transaction.makeBlock(block.offset, block.size, chunk.data, true, progressHandler, new RequestTransaction.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {

                destroyUploadRequestTransaction(transaction);
                completeHandler.complete(responseInfo, requestMetrics, response);
            }
        });
    }

    private void uploadChunk(final UploadBlock block,
                             final UploadData chunk,
                             final RequestProgressHandler progressHandler,
                             final PartsUploadPerformerCompleteHandler completeHandler) {

        final RequestTransaction transaction = createUploadRequestTransaction();
        transaction.uploadChunk(block.ctx, block.offset, chunk.data, chunk.offset, true, progressHandler, new RequestTransaction.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {

                destroyUploadRequestTransaction(transaction);
                completeHandler.complete(responseInfo, requestMetrics, response);
            }
        });

    }
}
