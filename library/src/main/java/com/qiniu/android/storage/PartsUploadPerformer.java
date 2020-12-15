package com.qiniu.android.storage;

import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.http.request.IUploadRegion;
import com.qiniu.android.http.request.RequestTransaction;
import com.qiniu.android.http.serverRegion.UploadDomainRegion;
import com.qiniu.android.utils.AsyncRun;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

abstract class PartsUploadPerformer {
    private static final String kRecordFileInfoKey = "recordFileInfo";
    private static final String kRecordZoneInfoKey = "recordZoneInfo";

    final String key;
    final String fileName;
    final File file;
    final RandomAccessFile randomAccessFile;

    final UpToken token;
    final UploadOptions options;
    final Configuration config;
    final Recorder recorder;
    final String recorderKey;

    private IUploadRegion targetRegion;
    protected IUploadRegion currentRegion;
    private double previousPercent;

    Long recoveredFrom;
    UploadFileInfo fileInfo;
    List<RequestTransaction> uploadTransactions;

    PartsUploadPerformer(File file,
                         String fileName,
                         String key,
                         UpToken token,
                         UploadOptions options,
                         Configuration config,
                         String recorderKey) {
        this.file = file;
        this.key = key;
        this.fileName = fileName;
        this.token = token;
        this.options = options;
        this.config = config;
        this.recorder = config.recorder;
        this.recorderKey = recorderKey;

        RandomAccessFile randomAccessFile = null;
        if (file != null) {
            try {
                randomAccessFile = new RandomAccessFile(file, "r");
            } catch (FileNotFoundException ignored) {
            }
        }
        this.randomAccessFile = randomAccessFile;
        this.initData();
    }

    void initData() {
        uploadTransactions = new ArrayList<>();
        recoverUploadInfoFromRecord();
        if (fileInfo == null) {
            fileInfo = getDefaultUploadFileInfo();
        }
    }

    boolean canReadFile() {
        return randomAccessFile != null;
    }

    void closeFile() {
        if (randomAccessFile != null) {
            try {
                randomAccessFile.close();
            } catch (IOException e) {
                try {
                    randomAccessFile.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    void switchRegion(IUploadRegion region) {
        if (fileInfo != null) {
            fileInfo.clearUploadState();
        }
        removeUploadInfoRecord();
        currentRegion = region;
        recoveredFrom = null;
        if (targetRegion == null) {
            targetRegion = region;
        }
    }

    void notifyProgress() {
        if (fileInfo == null) {
            return;
        }
        double percent = fileInfo.progress();
        if (percent > 0.95) {
            percent = 0.95;
        }
        if (percent > previousPercent) {
            previousPercent = percent;
        } else {
            percent = previousPercent;
        }

        final double notifyPercent = percent;
        AsyncRun.runInMain(new Runnable() {
            @Override
            public void run() {
                if (options != null && options.progressHandler != null) {
                    options.progressHandler.progress(key, notifyPercent);
                }
            }
        });
    }

    void recordUploadInfo() {
        String key = recorderKey;
        if (recorder == null || key == null || key.length() == 0) {
            return;
        }

        JSONObject zoneInfoJson = null;
        JSONObject fileInfoJson = null;
        if (currentRegion != null && currentRegion.getZoneInfo() != null) {
            zoneInfoJson = currentRegion.getZoneInfo().detailInfo;
        }
        if (fileInfo != null) {
            fileInfoJson = fileInfo.toJsonObject();
        }
        if (zoneInfoJson != null && fileInfo != null) {
            JSONObject info = new JSONObject();
            try {
                info.put(kRecordZoneInfoKey, zoneInfoJson);
                info.put(kRecordFileInfoKey, fileInfoJson);
            } catch (JSONException ignored) {
            }
            recorder.set(key, info.toString().getBytes());
        }
    }

    void removeUploadInfoRecord() {
        recoveredFrom = null;
        if (fileInfo != null) {
            fileInfo.clearUploadState();
        }
        if (recorder != null && recorderKey != null) {
            recorder.del(recorderKey);
        }
    }

    void recoverUploadInfoFromRecord() {
        String key = recorderKey;
        if (recorder == null || key == null || key.length() == 0 || file == null) {
            return;
        }

        byte[] data = recorder.get(key);
        if (data == null) {
            return;
        }

        try {
            JSONObject info = new JSONObject(new String(data));
            ZoneInfo zoneInfo = ZoneInfo.buildFromJson(info.getJSONObject(kRecordZoneInfoKey));
            UploadFileInfo recoverFileInfo = getFileFromJson(info.getJSONObject(kRecordFileInfoKey));
            if (zoneInfo != null && recoverFileInfo != null && !recoverFileInfo.isEmpty() && file != null &&
                    recoverFileInfo.size == file.length() &&
                    recoverFileInfo.modifyTime == file.lastModified()) {
                fileInfo = recoverFileInfo;
                UploadDomainRegion region = new UploadDomainRegion();
                region.setupRegionData(zoneInfo);
                currentRegion = region;
                targetRegion = region;
                recoveredFrom = (long) ((recoverFileInfo.progress() * recoverFileInfo.size));
            } else {
                recorder.del(key);
                currentRegion = null;
                targetRegion = null;
                recoveredFrom = null;
            }
        } catch (Exception e) {
            recorder.del(key);
            currentRegion = null;
            targetRegion = null;
            recoveredFrom = null;
        }
    }

    RequestTransaction createUploadRequestTransaction() {
        final RequestTransaction transaction = new RequestTransaction(config, options, targetRegion, currentRegion, key, token);
        synchronized (this) {
            if (uploadTransactions != null) {
                uploadTransactions.add(transaction);
            }
        }
        return transaction;
    }

    void destroyUploadRequestTransaction(RequestTransaction transaction) {
        if (transaction != null) {
            synchronized (this) {
                if (uploadTransactions != null) {
                    uploadTransactions.remove(transaction);
                }
            }
        }
    }

    abstract UploadFileInfo getDefaultUploadFileInfo();

    abstract UploadFileInfo getFileFromJson(JSONObject jsonObject);

    abstract void serverInit(PartsUploadPerformerCompleteHandler completeHandler);

    abstract void uploadNextDataCompleteHandler(PartsUploadPerformerDataCompleteHandler completeHandler);

    abstract void completeUpload(PartsUploadPerformerCompleteHandler completeHandler);

    interface PartsUploadPerformerCompleteHandler {
        void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response);
    }

    interface PartsUploadPerformerDataCompleteHandler {
        void complete(boolean stop, ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response);
    }
}
