package com.qiniu.android.storage;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.newHttp.RequestTranscation;
import com.qiniu.android.http.newHttp.UploadFileInfo;
import com.qiniu.android.http.newHttp.UploadRegion;
import com.qiniu.android.http.newHttp.handler.RequestProgressHandler;
import com.qiniu.android.http.newHttp.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.utils.GroupTaskThread;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class ConcurrentResumeUpload extends PartsUpload {

    private GroupTaskThread groupTaskThread;

    private double previousPercent;
    private ArrayList<RequestTranscation> uploadTranscations;

    private ResponseInfo uploadBlockErrorResponseInfo;
    private JSONObject uploadBlockErrorResponse;

    public ConcurrentResumeUpload(File file,
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
    public void prepareToUpload() {
        chunkSize = blockSize;
        super.prepareToUpload();
    }

    @Override
    public void startToUpload() {
        previousPercent = 0;
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
                            if (responseInfo != null && !responseInfo.isOK()){
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
                uploadBlockErrorResponseInfo = ResponseInfo.invalidArgument("regions error");
                uploadBlockErrorResponse = uploadBlockErrorResponseInfo.response;
            }
            completeHandler.complete();
            return;
        }

        UploadRegion currentRegion = getCurrentRegion();
        if (currentRegion == null){
            if (uploadBlockErrorResponseInfo == null){
                uploadBlockErrorResponseInfo = ResponseInfo.invalidArgument("server error");
                uploadBlockErrorResponse = uploadBlockErrorResponseInfo.response;
            }
            completeHandler.complete();
            return;
        }

        final UploadFileInfo.UploadData chunk = uploadFileInfo.nextUploadData();
        UploadFileInfo.UploadBlock block = chunk != null ? uploadFileInfo.blockWithIndex(chunk.blockIndex) : null;

        RequestProgressHandler progressHandler = new RequestProgressHandler() {
            @Override
            public void progress(long totalBytesWritten, long totalBytesExpectedToWrite) {
                chunk.progress = (double)totalBytesWritten / (double)totalBytesExpectedToWrite;
                double percent = uploadFileInfo.progress();
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
        };
        if (chunk != null){
            makeBlock(block, chunk, progressHandler, completeHandler);
        } else {
            completeHandler.complete();
        }
    }

    private void makeBlock(final UploadFileInfo.UploadBlock block,
                           final UploadFileInfo.UploadData chunk,
                           final RequestProgressHandler progressHandler,
                           final UploadBlockCompleteHandler completeHandler){

        chunk.isUploading = true;
        chunk.isCompleted = false;

        RequestTranscation transcation = createUploadRequestTranscation();
        transcation.makeBlock(block.offset, block.size, getDataWithChunk(chunk, block), true, progressHandler, new RequestTranscation.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
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
                    uploadBlockErrorResponse = response;
                    uploadBlockErrorResponseInfo = responseInfo;
                    setCurrentRegionRequestMetrics(requestMetrics);
                    completeHandler.complete();
                }
            }
        });
    }

    private void makeFile(final UploadFileCompleteHandler completeHandler){
        UploadFileInfo uploadFileInfo = getUploadFileInfo();

        final RequestTranscation transcation = createUploadRequestTranscation();
        ArrayList<String> contextsList = uploadFileInfo.allBlocksContexts();
        String[] contexts = contextsList.toArray(new String[contextsList.size()]);
        transcation.makeFile(uploadFileInfo.size, fileName,  contexts, true, new RequestTranscation.RequestCompleteHandler(){

            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {

                setCurrentRegionRequestMetrics(requestMetrics);
                destoryUploadRequestTranscation(transcation);
                completeHandler.complete(responseInfo, response);
            }
        });
    }

    private RequestTranscation createUploadRequestTranscation(){
        RequestTranscation transcation = new RequestTranscation(config, option, getTargetRegion(), getCurrentRegion(), key, token);
        uploadTranscations.add(transcation);
        return transcation;
    }

    private void destoryUploadRequestTranscation(RequestTranscation transcation){
        if (transcation != null){
            uploadTranscations.remove(transcation);
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
