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

    private final CountDownLatch signal = new CountDownLatch(1);
    private final CountDownLatch signal2 = new CountDownLatch(1);
    private volatile boolean cancelled;
    private volatile boolean failed;
    private volatile UploadManager uploadManager;
    private volatile String key;
    private volatile ResponseInfo info;
    private volatile JSONObject resp;
    private volatile UploadOptions options;

    @Override
    protected void setUp() throws Exception {
        File f = File.createTempFile("qiniutest", "b");
        String folder = f.getParent();
        FileRecorder fr = new FileRecorder(folder);
        uploadManager = new UploadManager(fr);
    }

    public void test400k() throws Throwable {
        templateFile(400, 0.2);
    }

    public void test700k() throws Throwable {
        templateFile(700, 0.2);
    }

    public void test1M() throws Throwable {
        templateFile(1024, 0.51);
    }

    public void test4M() throws Throwable {
        templateFile(4 * 1024, 0.8);
    }

    public void test8M1K() throws Throwable {
        templateFile(8 * 1024 + 1, 0.6);
    }

    public void testD400k() throws Throwable {
        templateData(400, 0.2);
    }

    public void testD700k() throws Throwable {
        templateData(700, 0.2);
    }

    public void testD1M() throws Throwable {
        templateData(1024, 0.51);
    }

    public void testD4M() throws Throwable {
        templateData(4 * 1024, 0.6);
    }

    private void templateFile(final int size, final double pos) throws Throwable {
        final File tempFile = TempFile.createFile(size);
        final String expectKey = "file_" + UUID.randomUUID().toString();
        cancelled = false;
        failed = false;
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:a", "test");
        params.put("x:b", "test2");

        Thread t = new Thread() {
            public void run() {
                try {
                    Thread.sleep(8 * 60 * 1000);
                    cancelled = true;
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
                    cancelled = true;
                }
                Log.i("qiniutest", pos + ": progress " + percent);
            }
        }, new UpCancellationSignal() {
            @Override
            public boolean isCancelled() {
                return cancelled;
            }
        });
        runTestOnUiThread(new Runnable() { // THIS IS THE KEY TO SUCCESS
            public void run() {
                uploadManager.put(tempFile, expectKey, TestConfig.token, new UpCompletionHandler() {
                    public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                        key = k;
                        info = rinfo;
                        resp = response;
                        signal.countDown();
                        Log.i("qiniutest", k + rinfo);
                    }
                }, options);
            }
        });

        try {
            signal.await(570, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 尝试获取info信息。
        // key == null ： 没进入 complete ？ 什么导致的？
        if (!expectKey.equals(key)) {
            //此处通不过， travis 会打印信息
            Assert.assertEquals("", info);
        }
        if (info == null || !info.isCancelled()) {
            //此处通不过， travis 会打印信息
            Assert.assertEquals("", info);
        }
        Assert.assertEquals(expectKey, key);
        Assert.assertFalse(info.isOK());
        Assert.assertTrue(info.isCancelled());
        Assert.assertNull(resp);

        TempFile.remove(tempFile);
    }

    private void templateData(final int size, final double pos) throws Throwable {
        final byte[] tempDate = TempFile.getByte(1024 * size);
        final String expectKey = "data_" + UUID.randomUUID().toString();
        cancelled = false;
        failed = false;
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:a", "test");
        params.put("x:b", "test2");

        Thread t = new Thread() {
            public void run() {
                try {
                    Thread.sleep(8 * 60 * 1000);
                    cancelled = true;
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
                    cancelled = true;
                }
                Log.i("qiniutest", pos + ": progress " + percent);
            }
        }, new UpCancellationSignal() {
            @Override
            public boolean isCancelled() {
                return cancelled;
            }
        });
        runTestOnUiThread(new Runnable() { // THIS IS THE KEY TO SUCCESS
            public void run() {
                uploadManager.put(tempDate, expectKey, TestConfig.token, new UpCompletionHandler() {
                    public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                        key = k;
                        info = rinfo;
                        resp = response;
                        signal.countDown();
                        Log.i("qiniutest", k + rinfo);
                    }
                }, options);
            }
        });

        try {
            signal.await(570, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 尝试获取info信息。
        // key == null ： 没进入 complete ？ 什么导致的？
        if (!expectKey.equals(key)) {
            //此处通不过， travis 会打印信息
            Assert.assertEquals("", info);
        }
        if (info == null || !info.isCancelled()) {
            //此处通不过， travis 会打印信息
            Assert.assertEquals("", info);
        }
        Assert.assertEquals(expectKey, key);
        Assert.assertFalse(info.isOK());
        Assert.assertTrue(info.isCancelled());
        Assert.assertNull(resp);
    }

}
