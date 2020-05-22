package com.qiniu.android.storage;

import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.request.UploadFileInfo;
import com.qiniu.android.http.request.UploadRegion;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

public class PartsUpload extends BaseUpload {
    private static final String kRecordFileInfoKey = "recordFileInfo";
    private static final String kRecordZoneInfoKey = "recordZoneInfo";

    public static long blockSize = 4*1024*1024;
    // 定制chunk大小 在执行run之前赋值
    public Long chunkSize;

    // 断点续传时，起始上传偏移, 只做为日志打点，不参与上传逻辑
    private Long recoveredFrom;
    private UploadFileInfo uploadFileInfo;

    private RandomAccessFile randomAccessFile;

    public PartsUpload(File file,
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

    public UploadFileInfo getUploadFileInfo(){
        return uploadFileInfo;
    };

    public RandomAccessFile getRandomAccessFile() {
        return randomAccessFile;
    }

    @Override
    public void prepareToUpload() {
        super.prepareToUpload();
        recoveryUploadInfoFromRecord();
        if (uploadFileInfo == null){
            uploadFileInfo = new UploadFileInfo(file.length(), PartsUpload.blockSize, getUploadChunkSize(), file.lastModified());
        }
    }

    @Override
    public boolean switchRegionAndUpload() {
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
    public void completeAction(ResponseInfo responseInfo, JSONObject response) {
        reportBlock();
        super.completeAction(responseInfo, response);
    }

    public void recordUploadInfo(){
        String key = recorderKey;
        if (recorder == null || key == null || key.length() == 0){
            return;
        }

        JSONObject zoneInfo = null;
        JSONObject fileInfo = null;
        UploadRegion currentRegion = getCurrentRegion();
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

    public void removeUploadInfoRecord(){
        recoveredFrom = null;
        if (uploadFileInfo != null){
            uploadFileInfo.clearUploadState();
        }
        if (recorder != null && recorderKey != null){
            recorder.del(recorderKey);
        }
    }

    private void recoveryUploadInfoFromRecord(){
        String key = recorderKey;
        if (recorder == null || key == null || key.length() == 0){
            return;
        }

        byte[] data = recorder.get(key);
        if (data == null){
            return;
        }

        try {
            JSONObject info = new JSONObject(data.toString());
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

    }
}