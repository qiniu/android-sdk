package com.qiniu.android;

import android.test.InstrumentationTestCase;
import android.util.Log;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestFileRecorder extends InstrumentationTestCase {
    final CountDownLatch signal = new CountDownLatch(1);
    final CountDownLatch signal2 = new CountDownLatch(1);
    private volatile boolean cancelled;
    private volatile boolean failed;
    private UploadManager uploadManager;
    private Configuration config;
    private volatile String key;
    private volatile ResponseInfo info;
    private volatile JSONObject resp;
    private volatile UploadOptions options;

    @Override
    protected void setUp() throws Exception {
        File f = File.createTempFile("qiniutest", "b");
        String folder = f.getParent();
        FileRecorder fr = new FileRecorder(folder);
        config = new Configuration.Builder().recorder(fr).build();
        uploadManager = new UploadManager(config);
    }

    private void template(final int size, final double pos) throws Throwable {
        final File tempFile = TempFile.createFile(size);
        final String expectKey = "rc=" + size + "k";
        cancelled = false;
        failed = false;
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:a", "test");
        params.put("x:b", "test2");
        options = new UploadOptions(params, null, false, new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                if (percent >= pos) {
                    cancelled = true;
                }
                Log.i("qiniutest", "progress " + percent);
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
                        Log.i("qiniutest", k + rinfo);
                        key = k;
                        info = rinfo;
                        resp = response;
                        signal.countDown();
                    }
                }, options);
            }
        });

        try {
            signal.await(600, TimeUnit.SECONDS); // wait for callback
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
        Assert.assertTrue(info.isCancelled());
        Assert.assertNull(resp);

        cancelled = false;
        options = new UploadOptions(null, null, false, new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                if (percent < pos - config.chunkSize / (size * 1024.0)) {
                    failed = true;
                }
                Log.i("qiniutest", "continue progress " + percent);
            }
        }, null);

        runTestOnUiThread(new Runnable() { // THIS IS THE KEY TO SUCCESS
            public void run() {
                uploadManager.put(tempFile, expectKey, TestConfig.token, new UpCompletionHandler() {
                    public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                        Log.i("qiniutest", k + rinfo);
                        key = k;
                        info = rinfo;
                        resp = response;
                        signal2.countDown();
                    }
                }, options);
            }
        });

        try {
            signal2.await(500, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 尝试获取info信息。
        // key == null ： 没进入 complete ？ 什么导致的？
        if (!expectKey.equals(key)) {
            //此处通不过， travis 会打印信息
            Assert.assertEquals("", info);
        }
        if (info == null || !info.isOK()) {
            //此处通不过， travis 会打印信息
            Assert.assertEquals("", info);
        }
        Assert.assertEquals(expectKey, key);
        Assert.assertTrue(info.isOK());
        Assert.assertTrue(!failed);
        Assert.assertNotNull(resp);

        TempFile.remove(tempFile);
    }

    public void test600k() throws Throwable {
        template(600, 0.7);
    }

    public void test700k() throws Throwable {
        template(700, 0.1);
    }

    public void test1M() throws Throwable {
        template(1024, 0.51);
    }

    public void test4M() throws Throwable {
        template(4 * 1024, 0.9);
    }

    public void test8M1K() throws Throwable {
        template(8 * 1024 + 1, 0.8);
    }
}
