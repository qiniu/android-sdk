package com.qiniu.android;

import android.test.AndroidTestCase;
import android.util.Log;

import com.qiniu.android.collect.Config;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.persistent.FileRecorder;
import com.qiniu.android.utils.UrlSafeBase64;

import junit.framework.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;

/**
 * Created by Simon on 12/6/16.
 */

public class ACollectUploadInfoTest  extends AndroidTestCase {

    private static long recordFileLastModified = 337;
    private static long recordFileSize = 0;

    @Override
    protected void setUp() throws Exception {
        testInit();
    }

    public static void testInit() {
        Config.isRecord = true;
        Config.isUpload = true;
        Config.interval = 0;
        Config.uploadThreshold = 4 * 1024;
        Config.maxRecordFileSize = 6 * 1024;
    }

    public static void testRecordFile0() {
        File recordFile = getRecordFile();
        if (recordFile.length() < Config.maxRecordFileSize && recordFile.length() > 10) {
            Assert.assertNotSame(recordFile.lastModified(), recordFileLastModified);
        }
        recordFileLastModified = recordFile.lastModified();
        int log = "200,CwIAAF4znMnpno0U,up.qiniu.com,183.131.7.18,80,383.0,1481014578,262144\n".length();

        Assert.assertTrue(recordFile.length() < Config.maxRecordFileSize + log * 2);

        Assert.assertTrue("文件大小平均值较大可能小于上传阀值", (recordFileSize + recordFile.length()) / 2 < Config.uploadThreshold + log * 2);
        recordFileSize = recordFile.length();
    }

    public static File getRecordFile() {
        String recordFileName = "_qiniu_record_file_hu3z9lo7anx03";
        File recordFile = new File(Config.recordDir, recordFileName);
        return recordFile;
    }

    public static void testRecordFile() {
        showRecordInfo();
        testRecordFile0();
    }

    public static void showRecordInfo() {
        File recordFile = getRecordFile();
//        Log.d("recordFile", "recordFile.getAbsolutePath(): " + recordFile.getAbsolutePath());
        Log.d("recordFile", "recordFile.length(): " + recordFile.length());
        Log.d("recordFile", "recordFile.lastModified(): " + new Date(recordFile.lastModified()));
//        showContent(recordFile);
    }

    private static void showContent(File recordFile) {
        FileReader fileReader = null;
        BufferedReader br = null;
        try {
            fileReader = new FileReader(recordFile);
            br = new BufferedReader(fileReader);
            String line = null;
            Log.d("recordFile", "recordFile content: start");
            while ((line = br.readLine()) != null) {
                Log.d("recordFile", line);
            }
            Log.d("recordFile", "recordFile content: end");
        } catch (Exception e) {

        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
