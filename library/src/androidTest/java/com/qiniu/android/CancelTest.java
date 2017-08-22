package com.qiniu.android;

import android.test.InstrumentationTestCase;
import android.util.Log;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpCancellationSignal;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.storage.persistent.FileRecorder;

import junit.framework.Assert;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by Simon on 2015/4/15.
 */
public class CancelTest extends InstrumentationTestCase {
    private volatile UploadManager uploadManager;
    private volatile UploadOptions options;

    @Override
    protected void setUp() throws Exception {
        File f = File.createTempFile("qiniutest", "b");
        String folder = f.getParent();
        FileRecorder fr = new FileRecorder(folder);
        uploadManager = new UploadManager(fr);
        ACollectUploadInfoTest.testInit();
    }


    public void testFile() throws Throwable {
        Temp[] ts = new Temp[]{templateFile(400, 0.2), templateFile(700, 0.2), templateFile(1024, 0.51), templateFile(4 * 1024, 0.5), templateFile(8 * 1024 + 1, 0.6)};
        checkTemp(ts, "testFile");
    }

    public void testData() throws Throwable {
        Temp[] ts = new Temp[]{templateData(400, 0.2), templateData(700, 0.2), templateData(1024, 0.51), templateData(4 * 1024 + 785, 0.5), templateData(4 * 1024, 0.5), templateData(8 * 1024, 0.6)};
        checkTemp(ts, "testData");
    }

    private void checkTemp(Temp[] ts, String type) {
        int failedCount = 0;
        Temp tt = null;

        for (int i = 0; i < ts.length; i++) {
            Temp t = ts[i];
            boolean b = t.expectKey.equals(t.key) && t.info.isCancelled() && (t.resp == null);
            if (!b) {
                tt = t;
                failedCount++;
            }
        }

        Log.d("qiniu_cancel", type + "   " + failedCount);
        if (failedCount > ts.length / 2) {
            String info = type + ": 共 " + ts.length + "个测试，至多允许 " + ts.length / 2 + " 失败，实际失败 " + failedCount + " 个： " + tt.info.toString();
            Assert.assertEquals(info, tt.expectKey, tt.key);
//            Assert.assertTrue(info, tt.info.isCancelled());
//            Assert.assertNull(info, tt.resp);
        }
    }


    private Temp templateFile(final int size, final double pos) throws Throwable {
        final CountDownLatch signal = new CountDownLatch(1);
        final File tempFile = TempFile.createFile(size);
        final String expectKey = "file_" + UUID.randomUUID().toString();
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:a", "test");
        params.put("x:b", "test2");

        final Temp temp = new Temp();
        temp.cancelled = false;

        Thread t = new Thread() {
            public void run() {
                try {
                    Thread.sleep(8 * 60 * 1000);
                    temp.cancelled = true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        t.setDaemon(true);
        t.start();

        options = new UploadOptions(params, null, false, new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                if (percent >= pos) {
                    temp.cancelled = true;
                }
                Log.i("qiniutest", pos + ": progress " + percent);
            }
        }, new UpCancellationSignal() {
            @Override
            public boolean isCancelled() {
                return temp.cancelled;
            }
        });
        runTestOnUiThread(new Runnable() { // THIS IS THE KEY TO SUCCESS
            public void run() {
                uploadManager.put(tempFile, expectKey, TestConfig.token_z0, new UpCompletionHandler() {
                    public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                        temp.expectKey = expectKey;
                        temp.key = k;
                        temp.info = rinfo;
                        temp.resp = response;
                        signal.countDown();
                        Log.i("qiniutest", k + rinfo);
                    }
                }, options);
            }
        });

        try {
            signal.await(570, TimeUnit.SECONDS); // wait for callback
            Assert.assertNotNull("timeout", temp.info);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        TempFile.remove(tempFile);

        ACollectUploadInfoTest.recordFileTest();

        return temp;
    }

    private Temp templateData(final int size, final double pos) throws Throwable {
        final CountDownLatch signal = new CountDownLatch(1);
        final byte[] tempDate = TempFile.getByte(1024 * size);
        final String expectKey = "data_" + UUID.randomUUID().toString();
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:a", "test");
        params.put("x:b", "test2");

        final Temp temp = new Temp();
        temp.cancelled = false;

        Thread t = new Thread() {
            public void run() {
                try {
                    Thread.sleep(8 * 60 * 1000);
                    temp.cancelled = true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        t.setDaemon(true);
        t.start();

        options = new UploadOptions(params, null, false, new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                if (percent >= pos) {
                    temp.cancelled = true;
                }
                Log.i("qiniutest", pos + ": progress " + percent);
            }
        }, new UpCancellationSignal() {
            @Override
            public boolean isCancelled() {
                return temp.cancelled;
            }
        });
        runTestOnUiThread(new Runnable() { // THIS IS THE KEY TO SUCCESS
            public void run() {
                uploadManager.put(tempDate, expectKey, TestConfig.token_z0, new UpCompletionHandler() {
                    public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                        temp.expectKey = expectKey;
                        temp.key = k;
                        temp.info = rinfo;
                        temp.resp = response;
                        signal.countDown();
                        Log.i("qiniutest", k + rinfo);
                    }
                }, options);
            }
        });

        try {
            signal.await(570, TimeUnit.SECONDS); // wait for callback
            Assert.assertNotNull("timeout", temp.info);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ACollectUploadInfoTest.recordFileTest();

        return temp;
    }

    private static class Temp {
        volatile ResponseInfo info;
        volatile JSONObject resp;
        volatile String key;
        volatile String expectKey;
        volatile boolean cancelled;
    }
}
