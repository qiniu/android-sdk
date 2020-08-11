package com.qiniu.android.storage;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.request.RequestTransaction;
import com.qiniu.android.http.request.UploadFileInfo;
import com.qiniu.android.http.request.IUploadRegion;
import com.qiniu.android.http.request.handler.RequestProgressHandler;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.utils.AsyncRun;
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

    private ResponseInfo uploadBlockErrorResponseInfo;
    private JSONObject uploadBlockErrorResponse;

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
        uploadTransactions = new ArrayList<RequestTransaction>();
        uploadBlockErrorResponseInfo = null;
        uploadBlockErrorResponse = null;

        GroupTaskThread.GroupTaskCompleteHandler completeHandler = new GroupTaskThread.GroupTaskCompleteHandler() {
            @Override
            public void complete() {
                UploadFileInfo uploadFileInfo = getUploadFileInfo();
                if (!uploadFileInfo.isAllUploaded() || uploadBlockErrorResponseInfo != null){
                    if (uploadBlockErrorResponseInfo.couldRetry() && config.allowBackupHost) {
                        boolean isSwitched = switchRegionAndUpload();
                        if (!isSwitched){
                            completeAction(uploadBlockErrorResponseInfo, uploadBlockErrorResponse);
                        }
                    } else {
                        completeAction(uploadBlockErrorResponseInfo, uploadBlockErrorResponse);
                    }
                } else {
                    makeFile(new UploadFileCompleteHandler() {
                        @Override
                        public void complete(ResponseInfo responseInfo, JSONObject response) {
                            if (responseInfo == null || !responseInfo.isOK()){
                                boolean isSwitched = switchRegionAndUpload();
                                if (!isSwitched){
                                    completeAction(responseInfo, response);
                                }
                            } else {
                                AsyncRun.runInMain(new Runnable() {
                                    @Override
                                    public void run() {
                                        option.progressHandler.progress(key, 1.0);
                                    }
                                });
                                removeUploadInfoRecord();
                                completeAction(responseInfo, response);
                            }
                        }
                    });
                }
            }
        };

        groupTaskThread = new GroupTaskThread(completeHandler);
        for (int i = 0; i < config.concurrentTaskCount; i++) {
            groupTaskThread.addTask(new GroupTaskThread.GroupTask() {
                @Override
                public void run(final GroupTaskThread.GroupTask task) {
                    uploadRestBlock(new UploadBlockCompleteHandler() {
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

    private void uploadRestBlock(UploadBlockCompleteHandler completeHandler){
        final UploadFileInfo uploadFileInfo = getUploadFileInfo();
        if (uploadFileInfo == null){
            if (uploadBlockErrorResponseInfo == null){
                uploadBlockErrorResponseInfo = ResponseInfo.invalidArgument("file error");
                uploadBlockErrorResponse = uploadBlockErrorResponseInfo.response;
            }
            completeHandler.complete();
            return;
        }

        IUploadRegion currentRegion = getCurrentRegion();
        if (currentRegion == null){
            if (uploadBlockErrorResponseInfo == null){
                uploadBlockErrorResponseInfo = ResponseInfo.invalidArgument("server error");
                uploadBlockErrorResponse = uploadBlockErrorResponseInfo.response;
            }
            completeHandler.complete();
            return;
        }

        synchronized (this) {
            final UploadFileInfo.UploadData chunk = uploadFileInfo.nextUploadData();
            UploadFileInfo.UploadBlock block = chunk != null ? uploadFileInfo.blockWithIndex(chunk.blockIndex) : null;

            RequestProgressHandler progressHandler = new RequestProgressHandler() {
                @Override
                public void progress(long totalBytesWritten, long totalBytesExpectedToWrite) {
                    chunk.progress = (double) totalBytesWritten / (double) totalBytesExpectedToWrite;
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
            if (chunk != null) {
                makeBlock(block, chunk, progressHandler, completeHandler);
            } else {
                completeHandler.complete();
            }
        }
    }

    private void makeBlock(final UploadFileInfo.UploadBlock block,
                           final UploadFileInfo.UploadData chunk,
                           final RequestProgressHandler progressHandler,
                           final UploadBlockCompleteHandler completeHandler){

        byte[] chunkData = getDataWithChunk(chunk, block);
        if (chunkData == null){
            uploadBlockErrorResponseInfo = ResponseInfo.localIOError("get chunk data error");
            uploadBlockErrorResponse = uploadBlockErrorResponseInfo.response;
            completeHandler.complete();
            return;
        }

        chunk.isUploading = true;
        chunk.isCompleted = false;

        RequestTransaction transaction = createUploadRequestTransaction();
        transaction.makeBlock(block.offset, block.size, chunkData, true, progressHandler, new RequestTransaction.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                addRegionRequestMetricsOfOneFlow(requestMetrics);

                String blockContext = null;
                if (response != null){
                    try {
                        blockContext = response.getString("ctx");
                    } catch (JSONException e) {}
                }
                if (responseInfo.isOK() && blockContext != null){
                    block.context = blockContext;
                    chunk.isUploading = false;
                    chunk.isCompleted = true;
                    recordUploadInfo();
                    uploadRestBlock(completeHandler);
                } else {
                    chunk.isUploading = false;
                    chunk.isCompleted = false;
                    uploadBlockErrorResponse = response;
                    uploadBlockErrorResponseInfo = responseInfo;
                    completeHandler.complete();
                }
            }
        });
    }

    private void makeFile(final UploadFileCompleteHandler completeHandler){
        UploadFileInfo uploadFileInfo = getUploadFileInfo();

        final RequestTransaction transaction = createUploadRequestTransaction();
        ArrayList<String> contextsList = uploadFileInfo.allBlocksContexts();
        String[] contexts = contextsList.toArray(new String[contextsList.size()]);
        transaction.makeFile(uploadFileInfo.size, fileName,  contexts, true, new RequestTransaction.RequestCompleteHandler(){

            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {

                addRegionRequestMetricsOfOneFlow(requestMetrics);
                destroyUploadRequestTransaction(transaction);
                completeHandler.complete(responseInfo, response);
            }
        });
    }

    private RequestTransaction createUploadRequestTransaction(){
        RequestTransaction transaction = new RequestTransaction(config, option, getTargetRegion(), getCurrentRegion(), key, token);
        uploadTransactions.add(transaction);
        return transaction;
    }

    private void destroyUploadRequestTransaction(RequestTransaction transaction){
        if (transaction != null){
            uploadTransactions.remove(transaction);
        }
    }

    private byte[] getDataWithChunk(UploadFileInfo.UploadData chunk,
                                    UploadFileInfo.UploadBlock block){
        RandomAccessFile randomAccessFile = getRandomAccessFile();
        if (randomAccessFile == null || chunk == null || block == null){
            return null;
        }
        byte[] data = new byte[(int)chunk.size];
        try {
            randomAccessFile.seek((chunk.offset + block.offset));
            randomAccessFile.read(data, 0, (int)chunk.size);
        } catch (IOException e) {
            data = null;
        }
        return data;
    }

    private interface UploadBlockCompleteHandler{
        void complete();
    }

    private interface UploadFileCompleteHandler{
        void complete(ResponseInfo responseInfo, JSONObject response);
    }
}
