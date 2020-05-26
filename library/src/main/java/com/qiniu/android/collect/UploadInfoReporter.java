package com.qiniu.android.collect;


import android.util.Log;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.http.request.RequestTranscation;
import com.qiniu.android.http.request.UploadFileInfo;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.utils.AsyncRun;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;

public class UploadInfoReporter {

    private final static String LogTag = "UploadInfoReporter";
    private ReportConfig config = ReportConfig.getInstance();
    private long lastReportTime = 0;
    private File recordDirectory = new File(config.recordDirectory);
    private File recorderFile = new File(config.recordDirectory + "/qiniu.log");
    private File recorderTempFile = new File(config.recordDirectory + "/qiniuTemp.log");
    private String X_Log_Client_Id;
    private RequestTranscation transcation;

    // 是否正在向服务上报中
    private boolean isReportting = false;

    private static UploadInfoReporter instance = new UploadInfoReporter();

    private UploadInfoReporter(){}

    public static UploadInfoReporter getInstance(){
        return instance;
    }

    public synchronized void report(ReportItem reportItem,
                                    final String tokenString){
        final String jsonString = reportItem.toJson();
        if (!checkReportAvailable() || jsonString == null){
            return;
        }

        AsyncRun.runInBack(new Runnable() {
            @Override
            public void run() {
                synchronized (this){
                    saveReportJsonString(jsonString);
                    reportToServerIfNeeded(tokenString);
                }
            }
        });
    }

    public void clean(){
        cleanRecorderFile();
        cleanTempLogFile();
    }

    private void cleanRecorderFile(){
        if (recorderFile.exists()){
            recorderFile.delete();
        }
    }

    private void cleanTempLogFile(){
        if (recorderTempFile.exists()){
            recorderTempFile.delete();
        }
    }

    private boolean checkReportAvailable(){
        if (!config.isReportEnable){
            return false;
        }
        if (config.maxRecordFileSize <= config.uploadThreshold){
            Log.e("UploadInfoReporter", "maxRecordFileSize must be larger than uploadThreshold");
            return false;
        }
        return true;
    }

    private void saveReportJsonString(final String jsonString){

        if (!recordDirectory.exists() && !recordDirectory.mkdirs()){
            return;
        }

        if (!recordDirectory.isDirectory()){
            Log.e(LogTag, "recordDirectory is not a directory");
            return;
        }

        if (!recorderFile.exists()){
            try {
                boolean isSuccess = recorderFile.createNewFile();
                if (!isSuccess){
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        if (recorderFile.length() > config.maxRecordFileSize){
            return;
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(recorderFile, true);
            fos.write((jsonString + "\n").getBytes());
            fos.flush();
        } catch (FileNotFoundException ignored) {
        } catch (IOException ignored) {
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {}
            }
        }
    }

    private void reportToServerIfNeeded(String tokenString){
        if (isReportting){
            return;
        }
        boolean needResport = false;
        long currentTime = new Date().getTime();

        if (recorderTempFile.exists()){
            needResport = true;
        } else if ((recorderFile.length() > config.uploadThreshold)
             && (lastReportTime == 0 || (currentTime - lastReportTime) > config.interval * 60)){
            boolean isSuccess = recorderFile.renameTo(recorderTempFile);
            if (isSuccess) {
                needResport = true;
            }
        }
        if (needResport && !this.isReportting){
            reportToServer(tokenString);
        }
    }

    private void reportToServer(String tokenString){

        isReportting = true;

        RequestTranscation transcation = createUploadRequestTranscation(tokenString);
        if (transcation == null){
            return;
        }

        byte[] logData = getLogData();
        if (logData == null && logData.length == 9){
            return;
        }

        transcation.reportLog(logData, X_Log_Client_Id, true, new RequestTranscation.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                if (responseInfo.isOK()){
                    lastReportTime = new Date().getTime();
                    if (X_Log_Client_Id == null
                            && responseInfo.responseHeader != null
                            && responseInfo.responseHeader.get("x-log-client-id") != null){
                        X_Log_Client_Id = responseInfo.responseHeader.get("x-log-client-id");
                    }
                    cleanTempLogFile();
                }
                isReportting = false;
            }
        });

    }

    private byte[] getLogData(){
        if (recorderTempFile == null || recorderTempFile.length() == 0){
            return null;
        }

        long length = recorderTempFile.length();
        RandomAccessFile randomAccessFile = null;
        byte[] data = null;
        try {
            randomAccessFile = new RandomAccessFile(recorderTempFile, "r");
            data = new byte[(int)length];
            randomAccessFile.seek(0);
            randomAccessFile.read(data);
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            data = null;
        }
        if (randomAccessFile != null){
            try {
                randomAccessFile.close();
            } catch (IOException e){}
        }
        return data;
    }

    private RequestTranscation createUploadRequestTranscation(String tokenString){
        if (config == null){
            return null;
        }
        UpToken token = UpToken.parse(tokenString);
        if (token == null){
            return null;
        }
        ArrayList<String> hosts = new ArrayList();
        hosts.add(config.serverURL);
        transcation = new RequestTranscation(hosts, token);
        return transcation;
    }

}
