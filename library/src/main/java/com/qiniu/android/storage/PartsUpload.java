package com.qiniu.android.storage;

import com.qiniu.android.collect.ReportItem;
import com.qiniu.android.collect.UploadInfoReporter;
import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.http.request.UploadFileInfo;
import com.qiniu.android.http.request.IUploadRegion;
import com.qiniu.android.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

abstract class PartsUpload extends BaseUpload {
    private static final String kRecordFileInfoKey = "recordFileInfo";
    private static final String kRecordZoneInfoKey = "recordZoneInfo";

    protected static long blockSize = 4*1024*1024;
    // 定制chunk大小 在执行run之前赋值
    protected Long chunkSize;

    // 断点续传时，起始上传偏移, 暂只做为日志打点，不参与上传逻辑
    private Long recoveredFrom;
    private UploadFileInfo uploadFileInfo;

    private RandomAccessFile randomAccessFile;

    protected PartsUpload(File file,
                          String key,
                          UpToken token,
                          UploadOptions option,
                          Configuration config,
                          Recorder recorder,
                          String recorderKey,
                          UpTaskCompletionHandler completionHandler) {
        super(file, key, token, option, config, recorder, recorderKey, completionHandler);
        RandomAccessFile randomAccessFile = null;
        if (file != null){
            try {
                randomAccessFile = new RandomAccessFile(file, "r");
            } catch (FileNotFoundException ignored) {}
        }
        this.randomAccessFile = randomAccessFile;
    }

    protected UploadFileInfo getUploadFileInfo(){
        return uploadFileInfo;
    }

    private void closeUploadFileInfo(){
        if (randomAccessFile != null){
            try {
                randomAccessFile.close();
            } catch (IOException ignored) {
            }
        }
    }

    protected RandomAccessFile getRandomAccessFile() {
        return randomAccessFile;
    }

    @Override
    protected int prepareToUpload() {
        int code = super.prepareToUpload();
        if (code != 0){
            return code;
        }

        recoverUploadInfoFromRecord();
        if (uploadFileInfo == null){
            uploadFileInfo = new UploadFileInfo(file.length(), PartsUpload.blockSize, getUploadChunkSize(), file.lastModified());
        }

        if (randomAccessFile == null){
            code = ResponseInfo.LocalIOError;
        }

        return code;
    }

    @Override
    protected boolean switchRegionAndUpload() {
        reportBlock();
        if (uploadFileInfo != null){
            uploadFileInfo.clearUploadState();
        }

        boolean isSwitched = super.switchRegionAndUpload();
        if (isSwitched){
            removeUploadInfoRecord();
        }
        return isSwitched;
    }

    @Override
    protected void completeAction(ResponseInfo responseInfo, JSONObject response) {
        reportBlock();
        closeUploadFileInfo();
        super.completeAction(responseInfo, response);
    }

    protected void recordUploadInfo(){
        String key = recorderKey;
        if (recorder == null || key == null || key.length() == 0){
            return;
        }

        JSONObject zoneInfo = null;
        JSONObject fileInfo = null;
        IUploadRegion currentRegion = getCurrentRegion();
        if (currentRegion != null && currentRegion.getZoneInfo() != null){
            zoneInfo = currentRegion.getZoneInfo().detailInfo;
        }
        if (uploadFileInfo != null){
            fileInfo = uploadFileInfo.toJsonObject();
        }
        if (zoneInfo != null && fileInfo != null){
            JSONObject info = new JSONObject();
            try {
                info.put(kRecordZoneInfoKey, zoneInfo);
                info.put(kRecordFileInfoKey, fileInfo);
            } catch (JSONException ignored) {}
            recorder.set(key, info.toString().getBytes());
        }
    }

    protected void removeUploadInfoRecord(){
        recoveredFrom = null;
        if (uploadFileInfo != null){
            uploadFileInfo.clearUploadState();
        }
        if (recorder != null && recorderKey != null){
            recorder.del(recorderKey);
        }
    }

    private void recoverUploadInfoFromRecord(){
        String key = recorderKey;
        if (recorder == null || key == null || key.length() == 0){
            return;
        }

        byte[] data = recorder.get(key);
        if (data == null){
            return;
        }

        try {
            JSONObject info = new JSONObject(new String(data));
            ZoneInfo zoneInfo = ZoneInfo.buildFromJson(info.getJSONObject(kRecordZoneInfoKey));
            UploadFileInfo fileInfo = UploadFileInfo.fileFromJson(info.getJSONObject(kRecordFileInfoKey));
            if (zoneInfo != null && fileInfo != null){
                insertRegionAtFirstByZoneInfo(zoneInfo);
                uploadFileInfo = fileInfo;
                recoveredFrom = (long)((fileInfo.progress() * fileInfo.size));
            } else {
                recorder.del(key);
            }
        } catch (JSONException e) {
            recorder.del(key);
        }
    }

    private long getUploadChunkSize(){
        if (chunkSize != null){
            return  chunkSize;
        } else {
            return config.chunkSize;
        }
    }

    private void reportBlock(){

        UploadRegionRequestMetrics metrics = getCurrentRegionRequestMetrics();
        if (metrics == null){
            metrics = new UploadRegionRequestMetrics(null);
        }

        String currentZoneRegionId = null;
        if (getCurrentRegion() != null && getCurrentRegion().getZoneInfo() != null && getCurrentRegion().getZoneInfo().regionId != null){
            currentZoneRegionId = getCurrentRegion().getZoneInfo().regionId;
        }
        String targetZoneRegionId = null;
        if (getTargetRegion() != null && getTargetRegion().getZoneInfo() != null && getTargetRegion().getZoneInfo().regionId != null){
            targetZoneRegionId = getTargetRegion().getZoneInfo().regionId;
        }

        ReportItem item = new ReportItem();
        item.setReport(ReportItem.LogTypeBlock, ReportItem.BlockKeyLogType);
        item.setReport((Utils.currentTimestamp()/1000), ReportItem.BlockKeyUpTime);
        item.setReport(currentZoneRegionId, ReportItem.BlockKeyTargetRegionId);
        item.setReport(targetZoneRegionId, ReportItem.BlockKeyCurrentRegionId);
        item.setReport(metrics.totalElapsedTime(), ReportItem.BlockKeyTotalElapsedTime);
        item.setReport(metrics.bytesSend(), ReportItem.BlockKeyBytesSent);
        item.setReport(recoveredFrom, ReportItem.BlockKeyRecoveredFrom);
        item.setReport(file.length(), ReportItem.BlockKeyFileSize);
        item.setReport(Utils.getCurrentProcessID(), ReportItem.BlockKeyPid);
        item.setReport(Utils.getCurrentThreadID(), ReportItem.BlockKeyTid);
        item.setReport(1, ReportItem.BlockKeyUpApiVersion);
        item.setReport(Utils.currentTimestamp(), ReportItem.BlockKeyClientTime);

        UploadInfoReporter.getInstance().report(item, token.token);
    }
}
