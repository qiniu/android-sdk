package com.qiniu.android.collect;


import android.util.Log;

import com.qiniu.android.utils.AsyncRun;
import com.qiniu.android.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;

public class UploadInfoReporter {

    private ReportConfig config = ReportConfig.getInstance();
    private long lastReportTime = 0;
    private File recorderFile = new File(config.recordDirectory + "/qiniu.log");
    private String X_Log_Client_Id;

    // 是否正在向服务上报中
    private boolean isReportting = false;

    private static UploadInfoReporter instance = new UploadInfoReporter();

    private UploadInfoReporter(){}

    public static UploadInfoReporter getInstance(){
        return instance;
    }

    public synchronized void report(ReportItem reportItem,
                       String token){
        final String jsonString = reportItem.toString();
        if (!checkReportAvailable() || jsonString == null){
            return;
        }

        AsyncRun.runInBack(new Runnable() {
            @Override
            public void run() {
                saveReportJsonString(jsonString);
                checkIfNeedReportToServer();
            }
        });
    }

    public void clean(){
        if (recorderFile.exists()){
            recorderFile.delete();
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

    private synchronized void saveReportJsonString(final String jsonString){

        if (!recorderFile.exists()){
            try {
                boolean isSuccess = recorderFile.createNewFile();
                if (!isSuccess){
                    return;
                }
            } catch (IOException e) {
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

    private synchronized void checkIfNeedReportToServer(){
        boolean needResport = false;
        long currentTime = new Date().getTime();
        if ((recorderFile.length() > config.maxRecordFileSize)
             && (lastReportTime == 0 || (currentTime - lastReportTime) > config.interval * 60)){
            needResport = true;
        }
        if (needResport && !this.isReportting){
            this.isReportting = true;
            reportToServer();
        }
    }

    private void reportToServer(){


    }

}
