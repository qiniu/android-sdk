package com.qiniu.android;

import android.test.AndroidTestCase;

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
    @Override
    protected void setUp() throws Exception {
        testInit();
    }

    public static void testInit() {
        Config.isRecord = true;
        Config.isUpload = true;
        Config.minInteval = 1;
        Config.uploadThreshold = 12 * 1024;
    }

    public static void showRecordFile() {
        String recordFileName = "_qiniu_record_file_hu3z9lo7anx03";
        File recordFile = new File(Config.recordDir, recordFileName);
        System.out.println("\n");
        System.out.println("recordFile.getAbsolutePath(): " + recordFile.getAbsolutePath());
        System.out.println("recordFile.length(): " + recordFile.length());
        System.out.println("recordFile.lastModified(): " + new Date(recordFile.lastModified()));
//        showContent(recordFile);
        System.out.println("\n");
    }

    public static void showContent(File recordFile) {
        FileReader fileReader = null;
        BufferedReader br = null;
        try {
            fileReader = new FileReader(recordFile);
            br = new BufferedReader(fileReader);
            String line = null;
            System.out.println("recordFile content: start");
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("recordFile content: end");
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
