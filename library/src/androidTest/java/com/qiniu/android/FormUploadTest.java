package com.qiniu.android;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.storage.Zone;

import junit.framework.Assert;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FormUploadTest extends InstrumentationTestCase {
    final CountDownLatch signal = new CountDownLatch(1);
    private UploadManager uploadManager;
    private volatile String key;
    private volatile ResponseInfo info;
    private volatile JSONObject resp;

    public void setUp() throws Exception {
        uploadManager = new UploadManager();
    }

    @SmallTest
    public void testHello() throws Throwable {
        final String expectKey = "你好;\"\r\n\r\n\r\n";
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:foo", "fooval");
        final UploadOptions opt = new UploadOptions(params, null, true, null, null);

        uploadManager.put("hello".getBytes(), expectKey, TestConfig.token, new UpCompletionHandler() {
            public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                Log.i("qiniutest", k + rinfo);
                key = k;
                info = rinfo;
                resp = response;
                signal.countDown();
            }
        }, opt);


        try {
            signal.await(120, TimeUnit.SECONDS); // wait for callback
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
        Assert.assertEquals("/", info.path);
    }

    @SmallTest
    public void testNoKey() throws Throwable {
        final String expectKey = null;
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:foo", "fooval");
        final UploadOptions opt = new UploadOptions(params, null, true, null, null);
        runTestOnUiThread(new Runnable() { // THIS IS THE KEY TO SUCCESS
            public void run() {
                uploadManager.put("hello".getBytes(), expectKey, TestConfig.token, new UpCompletionHandler() {
                    public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                        Log.i("qiniutest", k + rinfo);
                        key = k;
                        info = rinfo;
                        resp = response;
                        signal.countDown();
                    }
                }, opt);
            }
        });

        try {
            signal.await(120, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 尝试获取info信息。
        if (info == null || !info.isOK()) {
            //此处通不过， travis 会打印信息
            Assert.assertEquals("", info);
        }
        Assert.assertEquals(expectKey, key);
        Assert.assertTrue(info.isOK());
        Assert.assertNotNull(info.reqId);
        Assert.assertEquals("/", info.path);
        Assert.assertNotNull(resp);
        Assert.assertEquals("Fqr0xh3cxeii2r7eDztILNmuqUNN", resp.optString("key", ""));
    }

    @SmallTest
    public void testInvalidToken() throws Throwable {
        final String expectKey = "你好";
        runTestOnUiThread(new Runnable() { // THIS IS THE KEY TO SUCCESS
            public void run() {
                uploadManager.put("hello".getBytes(), expectKey, "invalid", new UpCompletionHandler() {
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
            signal.await(120, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(expectKey, key);
        Assert.assertEquals(ResponseInfo.InvalidToken, info.statusCode);
        Assert.assertNotNull(info.reqId);
        Assert.assertNull(resp);
    }

    @SmallTest
    public void testNoData() throws Throwable {
        final String expectKey = "你好";

        uploadManager.put((byte[]) null, expectKey, "invalid", new UpCompletionHandler() {
            public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                Log.i("qiniutest", k + rinfo);
                key = k;
                info = rinfo;
                resp = response;
                signal.countDown();
            }
        }, null);


        try {
            signal.await(120, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(expectKey, key);
        Assert.assertEquals(ResponseInfo.InvalidArgument, info.statusCode);
        Assert.assertNull(resp);
    }

    @SmallTest
    public void testNoToken() throws Throwable {
        final String expectKey = "你好";
        runTestOnUiThread(new Runnable() { // THIS IS THE KEY TO SUCCESS
            public void run() {
                uploadManager.put(new byte[0], expectKey, null, new UpCompletionHandler() {
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
            signal.await(120, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(expectKey, key);
        Assert.assertEquals(ResponseInfo.InvalidArgument, info.statusCode);
        Assert.assertNull(resp);
    }

    @SmallTest
    public void testEmptyToken() throws Throwable {
        final String expectKey = "你好";
        runTestOnUiThread(new Runnable() { // THIS IS THE KEY TO SUCCESS
            public void run() {
                uploadManager.put(new byte[0], expectKey, "", new UpCompletionHandler() {
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
            signal.await(120, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(expectKey, key);
        Assert.assertEquals(ResponseInfo.InvalidArgument, info.statusCode);
        Assert.assertNull(resp);
    }

    @MediumTest
    public void testFile() throws Throwable {
        final String expectKey = "世/界";
        final File f = TempFile.createFile(1);
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:foo", "fooval");
        final UploadOptions opt = new UploadOptions(params, null, true, null, null);
        uploadManager.put(f, expectKey, TestConfig.token, new UpCompletionHandler() {
            public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                Log.i("qiniutest", k + rinfo);
                key = k;
                info = rinfo;
                resp = response;
                signal.countDown();
            }
        }, opt);

        try {
            signal.await(130, TimeUnit.SECONDS); // wait for callback
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

    @SmallTest
    public void testNoComplete() {
        Exception error = null;
        try {
            uploadManager.put(new byte[0], null, null, null, null);
        } catch (Exception e) {
            error = e;
        }
        Assert.assertNotNull(error);
        Assert.assertTrue(error instanceof IllegalArgumentException);
        Assert.assertEquals("no UpCompletionHandler", error.getMessage());

        error = null;
        try {
            uploadManager.put("", null, null, null, null);
        } catch (Exception e) {
            error = e;
        }
        Assert.assertNotNull(error);
        Assert.assertTrue(error instanceof IllegalArgumentException);
        Assert.assertEquals("no UpCompletionHandler", error.getMessage());
    }

    @SmallTest
    public void testIpBack() throws Throwable {

        Configuration c = new Configuration.Builder()
                .zone(new Zone("upwelcome.qiniu.com", Zone.zone0.upHostBackup, Zone.zone0.upIp))
                .build();
        UploadManager uploadManager2 = new UploadManager(c);
        final String expectKey = "你好;\"\r\n\r\n\r\n";
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:foo", "fooval");
        final UploadOptions opt = new UploadOptions(params, null, true, null, null);

        uploadManager2.put("hello".getBytes(), expectKey, TestConfig.token, new UpCompletionHandler() {
            public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                Log.i("qiniutest", k + rinfo);
                key = k;
                info = rinfo;
                resp = response;
                signal.countDown();
            }
        }, opt);


        try {
            signal.await(120, TimeUnit.SECONDS); // wait for callback
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
    }

    @SmallTest
    public void testPortBackup() throws Throwable {
        Configuration c = new Configuration.Builder()
                .zone(new Zone("upload.qiniu.com", Zone.zone0.upHostBackup, Zone.zone0.upIp))
                .upPort(9999)
                .build();
        UploadManager uploadManager2 = new UploadManager(c);
        final String expectKey = "你好;\"\r\n\r\n\r\n";
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:foo", "fooval");
        final UploadOptions opt = new UploadOptions(params, null, true, null, null);

        uploadManager2.put("hello".getBytes(), expectKey, TestConfig.token, new UpCompletionHandler() {
            public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                Log.i("qiniutest", k + rinfo);
                key = k;
                info = rinfo;
                resp = response;
                signal.countDown();
            }
        }, opt);


        try {
            signal.await(120, TimeUnit.SECONDS); // wait for callback
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
    }

    @SmallTest
    public void testDnsHijacking() throws Throwable {
        Configuration c = new Configuration.Builder()
                .zone(new Zone("uphijacktest.qiniu.com", Zone.zone0.upHostBackup, Zone.zone0.upIp))
                .build();
        UploadManager uploadManager2 = new UploadManager(c);
        final String expectKey = "你好;\"\r\n\r\n\r\n";
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:foo", "fooval");
        final UploadOptions opt = new UploadOptions(params, null, true, null, null);

        uploadManager2.put("hello".getBytes(), expectKey, TestConfig.token, new UpCompletionHandler() {
            public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                Log.i("qiniutest", k + rinfo);
                key = k;
                info = rinfo;
                resp = response;
                signal.countDown();
            }
        }, opt);


        try {
            signal.await(120, TimeUnit.SECONDS); // wait for callback
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
    }
}
