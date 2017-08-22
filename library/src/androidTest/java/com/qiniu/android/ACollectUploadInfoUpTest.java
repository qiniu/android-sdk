package com.qiniu.android;

import android.test.AndroidTestCase;
import android.util.Log;

import com.qiniu.android.collect.Config;

import junit.framework.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Simon on 12/6/16.
 */

public class ACollectUploadInfoUpTest extends AndroidTestCase {

    private static long recordFileLastModified = 337l;
    private static Queue<Long> queue = new LinkedList<Long>() {{
        offer(1l);
        offer(2l);
        offer(3l);
        offer(4l);
    }};

    public static void testInit() {
        Config.isRecord = true;
        Config.isUpload = true;
        Config.interval = 0;
        Config.uploadThreshold = 2 * 1024;
        Config.maxRecordFileSize = 3 * 1024;
    }

    public static void recordFile() {
        File recordFile = getRecordFile();
        if (recordFile.length() < Config.maxRecordFileSize && recordFile.length() > 10) {
            Assert.assertNotSame(recordFile.lastModified(), recordFileLastModified);
        }
        recordFileLastModified = recordFile.lastModified();

        int log = "200,CwIAAF4znMnpno0U,up.qiniu.com,183.131.7.18,80,383.0,1481014578,262144,form\n".length();

        long fileSize = recordFile.length();
        putRecordFileSize(fileSize);
        long avgSize = getRecordFileAvgSize();

//        Assert.assertTrue(fileSize < Config.maxRecordFileSize + log * 2);

        String sizes = getRecordFileSizes();
        System.out.println("RecordFileSize: " + sizes);
//        Assert.assertTrue("文件大小有变化，不大可能一直相同。" + sizes, !isRecordFileSizeAllSame());
//        Assert.assertTrue("有上传，上传后清理文件，文件大小平均值不大可能大于上传阀值。" + sizes, avgSize < Config.uploadThreshold + log * 2);
    }

    private static void putRecordFileSize(long size) {
        queue.offer(size);
        if (queue.size() > 4) {
            queue.poll();
        }
    }

    private static long getRecordFileAvgSize() {
        long sum = 0;
        long count = 0;
        Iterator<Long> it = queue.iterator();
        while (it.hasNext()) {
            count += 1;
            sum += it.next();
        }
        return sum / count;
    }

    private static boolean isRecordFileSizeAllSame() {
        long size = -1;
        boolean same = true;
        Iterator<Long> it = queue.iterator();
        while (it.hasNext()) {
            long temp = it.next();
            if (size > 0) {
                same = same && (size == temp);
            }
            size = temp;
        }
        return same;
    }

    private static String getRecordFileSizes() {
        StringBuilder ss = new StringBuilder("");
        Iterator<Long> it = queue.iterator();
        while (it.hasNext()) {
            ss.append(it.next()).append(", ");
        }
        return ss.toString();
    }

    public static File getRecordFile() {
        String recordFileName = "_qiniu_record_file_hs5z9lo7anx03";
        File recordFile = new File(Config.recordDir, recordFileName);
        return recordFile;
    }

    public static void recordFileTest() {
        showRecordInfo();
        recordFile();
    }

    public static void showRecordInfo() {
        File recordFile = getRecordFile();
        Log.d("recordFile", "recordFile.getAbsolutePath(): " + recordFile.getAbsolutePath());
        Log.d("recordFile", "recordFile.length(): " + recordFile.length());
        Log.d("recordFile", "recordFile.lastModified(): " + new Date(recordFile.lastModified()));
        showContent(recordFile);
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

    @Override
    protected void setUp() throws Exception {
        testInit();
    }
}
