package com.qiniu.android;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;

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
    private String key;
    private ResponseInfo info;
    private JSONObject resp;

    public void setUp() throws Exception {
        uploadManager = new UploadManager();
    }

    @SmallTest
    public void testHello() throws Throwable {
        final String expectKey = "你好";
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
            signal.await(60, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(expectKey, key);
        Assert.assertTrue(info.isOK());
        Assert.assertNotNull(info.reqId);
        Assert.assertNotNull(resp);
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
            signal.await(60, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(expectKey, key);
        Assert.assertTrue(info.isOK());
        Assert.assertNotNull(info.reqId);
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
            signal.await(60, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(expectKey, key);
        Assert.assertEquals(401, info.statusCode);
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
            signal.await(60, TimeUnit.SECONDS); // wait for callback
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
            signal.await(60, TimeUnit.SECONDS); // wait for callback
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
            signal.await(60, TimeUnit.SECONDS); // wait for callback
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
            signal.await(120, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
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

}
