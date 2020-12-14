package com.qiniu.android.storage;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.http.request.RequestTransaction;
import com.qiniu.android.http.request.handler.RequestProgressHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

class PartsUploadPerformerV1 extends PartsUploadPerformer {

    private static long BlockSize = 4 * 1024 * 1024;

    PartsUploadPerformerV1(File file,
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
        long size = 0;
        long modifyTime = 0;
        ArrayList<UploadBlock> uploadBlocks = new ArrayList<UploadBlock>();
        try {
            size = jsonObject.getLong("size");
            modifyTime = jsonObject.getLong("modifyTime");
            JSONArray blockJsonArray = jsonObject.getJSONArray("uploadBlocks");
            for (int i = 0; i < blockJsonArray.length(); i++) {
                JSONObject blockJson = blockJsonArray.getJSONObject(i);
                UploadBlock block = UploadBlock.blockFromJson(blockJson);
                if (block != null) {
                    uploadBlocks.add(block);
                }
            }
        } catch (JSONException e) {
        }

        UploadFileInfoPartV1 fileInfo = new UploadFileInfoPartV1(size, modifyTime, uploadBlocks);
        return fileInfo;
    }

    @Override
    UploadFileInfo getDefaultUploadFileInfo() {
        return new UploadFileInfoPartV1(file.length(), BlockSize, getUploadChunkSize(), file.lastModified());
    }

    @Override
    void serverInit(PartsUploadPerformerCompleteHandler completeHandler) {
        ResponseInfo responseInfo = ResponseInfo.successResponse();
        completeHandler.complete(responseInfo, null, null);
    }

    @Override
    void uploadNextDataCompleteHandler(final PartsUploadPerformerDataCompleteHandler completeHandler) {
        UploadFileInfoPartV1 uploadFileInfo = (UploadFileInfoPartV1) fileInfo;
        UploadBlock block = null;
        UploadData chunk = null;

        synchronized (this) {
            block = uploadFileInfo.nextUploadBlock();
            if (block != null) {
                chunk = block.nextUploadData();
                if (chunk != null) {
                    chunk.isUploading = true;
                    chunk.isCompleted = false;
                }
            }
        }
        if (block == null || chunk == null) {
            completeHandler.complete(true, null, null, null);
            return;
        }

        chunk.data = getDataWithChunk(chunk, block);
        if (chunk.data == null) {
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

                uploadChunk.data = null;

                String blockContext = null;
                if (response != null) {
                    try {
                        blockContext = response.getString("ctx");
                    } catch (JSONException e) {
                    }
                }
                if (responseInfo.isOK() && blockContext != null) {
                    uploadBlock.context = blockContext;
                    uploadChunk.isUploading = false;
                    uploadChunk.isCompleted = true;
                    recordUploadInfo();
                } else {
                    uploadChunk.isUploading = false;
                    uploadChunk.isCompleted = false;

                }
                completeHandler.complete(false, responseInfo, requestMetrics, response);
            }
        };

        if (uploadChunk.isFirstData()) {
            makeBlock(uploadBlock, uploadChunk, progressHandler, completeHandlerP);
        } else {
            uploadChunk(uploadBlock, uploadChunk, progressHandler, completeHandlerP);
        }
    }

    @Override
    void completeUpload(final PartsUploadPerformerCompleteHandler completeHandler) {
        UploadFileInfoPartV1 uploadFileInfo = (UploadFileInfoPartV1) fileInfo;

        ArrayList<String> contextsList = uploadFileInfo.allBlocksContexts();

        if (contextsList == null || contextsList.size() == 0) {
            ResponseInfo responseInfo = ResponseInfo.invalidArgument("block ctx invalid");
            completeHandler.complete(responseInfo, null, responseInfo.response);
            return;
        }

        String[] contexts = contextsList.toArray(new String[contextsList.size()]);

        final RequestTransaction transaction = createUploadRequestTransaction();
        transaction.makeFile(uploadFileInfo.size, fileName, contexts, true, new RequestTransaction.RequestCompleteHandler() {

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

    private byte[] getDataWithChunk(UploadData chunk,
                                    UploadBlock block) {
        if (randomAccessFile == null || chunk == null || block == null) {
            return null;
        }
        byte[] data = new byte[(int) chunk.size];
        try {
            synchronized (randomAccessFile) {
                randomAccessFile.seek((chunk.offset + block.offset));
                randomAccessFile.read(data, 0, (int) chunk.size);
            }
        } catch (IOException e) {
            data = null;
        }
        return data;
    }

    private long getUploadChunkSize() {
        if (config.useConcurrentResumeUpload) {
            return BlockSize;
        } else {
            return config.chunkSize;
        }
    }
}
