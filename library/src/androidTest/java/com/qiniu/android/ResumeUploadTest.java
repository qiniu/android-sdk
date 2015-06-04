package com.qiniu.android;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.Zone;

import junit.framework.Assert;

import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ResumeUploadTest extends InstrumentationTestCase {

    final CountDownLatch signal = new CountDownLatch(1);
    private UploadManager uploadManager;
    private volatile String key;
    private volatile ResponseInfo info;
    private volatile JSONObject resp;

    public void setUp() throws Exception {
        Configuration config = new Configuration.Builder().upPort(9999).build();
        uploadManager = new UploadManager(config);
    }

    private void template(int size) throws Throwable {
        final String expectKey = "r=" + size + "k";
        final File f = TempFile.createFile(size);
        runTestOnUiThread(new Runnable() { // THIS IS THE KEY TO SUCCESS
            public void run() {
                uploadManager.put(f, expectKey, TestConfig.token, new UpCompletionHandler() {
                    public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                        Log.i("qiniutest", k + rinfo);
                        key = k;
                        info = rinfo;
                        resp = response;
                        signal.countDown();
                    }
                }, null);
            }
        });

        try {
            signal.await(500, TimeUnit.SECONDS); // wait for callback
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
        Assert.assertNotNull(info.reqId);
        Assert.assertNotNull(resp);
        TempFile.remove(f);
    }

    private void template2(int size) throws Throwable {
        final String expectKey = "r=" + size + "k";
        final File f = TempFile.createFile(size);

        uploadManager.put(f, expectKey, TestConfig.token, new UpCompletionHandler() {
            public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                Log.i("qiniutest", k + rinfo);
                key = k;
                info = rinfo;
                resp = response;
                signal.countDown();
            }
        }, null);

        try {
            signal.await(500, TimeUnit.SECONDS); // wait for callback
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
        Assert.assertNotNull(info.reqId);
        Assert.assertNotNull(resp);
        TempFile.remove(f);
    }

    private void templateHijack(int size) throws Throwable {
        final String expectKey = "r=" + size + "k";
        final File f = TempFile.createFile(size);

        Configuration c = new Configuration.Builder()
                .zone(new Zone("uphijacktest.qiniu.com", Zone.zone0.upHostBackup, Zone.zone0.upIp))
                .build();
        UploadManager uploadManager = new UploadManager(c);

        uploadManager.put(f, expectKey, TestConfig.token, new UpCompletionHandler() {
            public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                Log.i("qiniutest", k + rinfo);
                key = k;
                info = rinfo;
                resp = response;
                signal.countDown();
            }
        }, null);

        try {
            signal.await(500, TimeUnit.SECONDS); // wait for callback
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
        Assert.assertNotNull(info.reqId);
        Assert.assertNotNull(resp);
        TempFile.remove(f);
    }

    @MediumTest
    public void test600k() throws Throwable {
        template(600);
    }

    @MediumTest
    public void test600k2() throws Throwable {
        template2(600);
    }

    @LargeTest
    public void test4M() throws Throwable {
        template(1024 * 4);
    }

    @LargeTest
    public void test2MHijack() throws Throwable {
        templateHijack(1024 * 2);
    }


//    @LargeTest
//    public void test8M1k() throws Throwable{
//        template(1024*8+1);
//    }
}
