package com.qiniu.android.storage;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.http.request.RequestTransaction;
import com.qiniu.android.http.request.handler.RequestProgressHandler;
import com.qiniu.android.utils.LogUtil;
import com.qiniu.android.utils.StringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

class PartsUploadPerformerV1 extends PartsUploadPerformer {

    private static int BlockSize = 4 * 1024 * 1024;

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
            } catch (IOException e) {
                //todo: 此处可能导致后面无法恢复
            }

            if (block != null) {
                chunk = block.nextUploadData();
                if (chunk != null) {
                    chunk.isUploading = true;
                    chunk.isCompleted = false;
                }
            }
        }
        if (block == null || chunk == null) {
            LogUtil.i("key:" + StringUtils.toNonnullString(key) + " no chunk left");

            ResponseInfo responseInfo = ResponseInfo.sdkInteriorError("no chunk left");
            completeHandler.complete(true, responseInfo, null, null);
            return;
        }

//        chunk.data = getChunkDataWithRetry(chunk, block);
        if (chunk.data == null) {
            LogUtil.i("key:" + StringUtils.toNonnullString(key) + " no chunk left");

            chunk.isUploading = false;
            chunk.isCompleted = false;
            ResponseInfo responseInfo = ResponseInfo.localIOError("get data error");
            completeHandler.complete(true, responseInfo, null, null);
            return;
        }

        final UploadBlock uploadBlock = block;
        final UploadData uploadChunk = chunk;
        RequestProgressHandler progressHandler = new RequestProgressHandler() {
            @Override
            public void progress(long totalBytesWritten, long totalBytesExpectedToWrite) {
                uploadChunk.progress = (double) totalBytesWritten / (double) totalBytesExpectedToWrite;
                notifyProgress();
            }
        };
        PartsUploadPerformerCompleteHandler completeHandlerP = new PartsUploadPerformerCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {

                String blockContext = null;
                if (response != null) {
                    try {
                        blockContext = response.getString("ctx");
                    } catch (JSONException e) {
                    }
                }
                if (responseInfo.isOK() && blockContext != null) {
                    uploadChunk.data = null;

                    uploadBlock.context = blockContext;
                    uploadChunk.progress = 1;
                    uploadChunk.isUploading = false;
                    uploadChunk.isCompleted = true;
                    recordUploadInfo();
                    notifyProgress();
                } else {
                    uploadChunk.isUploading = false;
                    uploadChunk.isCompleted = false;

                }
                completeHandler.complete(false, responseInfo, requestMetrics, response);
            }
        };

        if (uploadChunk.isFirstData()) {
            LogUtil.i("key:" + StringUtils.toNonnullString(key) + " makeBlock");
            makeBlock(uploadBlock, uploadChunk, progressHandler, completeHandlerP);
        } else {
            LogUtil.i("key:" + StringUtils.toNonnullString(key) + " makeBlock");
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
        transaction.uploadChunk(block.context, block.offset, chunk.data, chunk.offset, true, progressHandler, new RequestTransaction.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {

                destroyUploadRequestTransaction(transaction);
                completeHandler.complete(responseInfo, requestMetrics, response);
            }
        });

    }

//    private byte[] getChunkDataWithRetry(UploadData chunk, UploadBlock block) {
//        byte[] uploadData = null;
//
//        int maxTime = 3;
//        int index = 0;
//        while (index < maxTime) {
//            uploadData = getChunkData(chunk, block);
//            if (uploadData != null) {
//                break;
//            }
//            index ++;
//        }
//
//        return uploadData;
//    }

//    private synchronized byte[] getChunkData(UploadData chunk, UploadBlock block) {
//        if (randomAccessFile == null || chunk == null || block == null) {
//            return null;
//        }
//        int readSize = 0;
//        byte[] data = new byte[chunk.size];
//        try {
//            randomAccessFile.seek((chunk.offset + block.offset));
//            while (readSize < chunk.size) {
//                int ret = randomAccessFile.read(data, readSize, (chunk.size - readSize));
//                if (ret < 0) {
//                    break;
//                }
//                readSize += ret;
//            }
//
//            // 读数据非预期
//            if (readSize != chunk.size) {
//                data = null;
//            }
//        } catch (IOException e) {
//            data = null;
//        }
//        return data;
//    }
//
//    private int getUploadChunkSize() {
//        if (config.useConcurrentResumeUpload) {
//            return BlockSize;
//        } else {
//            return config.chunkSize;
//        }
//    }
}
