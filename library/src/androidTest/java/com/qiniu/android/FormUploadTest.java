package com.qiniu.android;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.qiniu.android.common.ServiceAddress;
import com.qiniu.android.common.Zone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.utils.Etag;

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
        byte[] b = "hello".getBytes();
        uploadManager.put(b, expectKey, TestConfig.token, new UpCompletionHandler() {
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
            Assert.assertNotNull("timeout", info);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(info.toString(), expectKey, key);
        Assert.assertTrue(info.toString(), info.isOK());
        Assert.assertNotNull(info.reqId);
        Assert.assertNotNull(resp);

        String hash = resp.getString("hash");
        Assert.assertEquals(hash, Etag.data(b));
    }

    @SmallTest
    public void test0Data() throws Throwable {
        final String expectKey = "你好;\"\r\n\r\n\r\n";
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:foo", "fooval");
        final UploadOptions opt = new UploadOptions(params, null, true, null, null);

        uploadManager.put("".getBytes(), expectKey, TestConfig.token, new UpCompletionHandler() {
            public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                Log.i("qiniutest", k + rinfo);
                key = k;
                info = rinfo;
                resp = response;
                signal.countDown();
            }
        }, opt);

        try {
            signal.await(10, TimeUnit.SECONDS); // wait for callback
            Assert.assertNotNull("timeout", info);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(info.toString(), ResponseInfo.ZeroSizeFile, info.statusCode);
        Assert.assertEquals(info.toString(), expectKey, key);
        Assert.assertFalse(info.toString(), info.isOK());
        Assert.assertEquals(info.toString(), "", info.reqId);
        Assert.assertNull(resp);
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
            Assert.assertNotNull("timeout", info);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(info.toString(), expectKey, key);
        Assert.assertTrue(info.toString(), info.isOK());

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
            signal.await(120, TimeUnit.SECONDS); // wait for callback
            Assert.assertNotNull("timeout", info);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(info.toString(), expectKey, key);
        Assert.assertEquals(info.toString(), ResponseInfo.InvalidToken, info.statusCode);
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
            assertNotNull("timeout", info);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(info.toString(), expectKey, key);
        Assert.assertEquals(info.toString(), ResponseInfo.InvalidArgument,
                info.statusCode);
        Assert.assertNull(resp);
    }

    @SmallTest
    public void testNoToken() throws Throwable {
        final String expectKey = "你好";
        runTestOnUiThread(new Runnable() { // THIS IS THE KEY TO SUCCESS
            public void run() {
                uploadManager.put(new byte[1], expectKey, null, new UpCompletionHandler() {
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
            Assert.assertNotNull("timeout", info);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(info.toString(), expectKey, key);
        Assert.assertEquals(info.toString(), ResponseInfo.InvalidArgument, info.statusCode);
        Assert.assertNull(resp);
    }

    @SmallTest
    public void testEmptyToken() throws Throwable {
        final String expectKey = "你好";
        runTestOnUiThread(new Runnable() { // THIS IS THE KEY TO SUCCESS
            public void run() {
                uploadManager.put(new byte[1], expectKey, "", new UpCompletionHandler() {
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
            Assert.assertNotNull("timeout", info);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(info.toString(), expectKey, key);
        Assert.assertEquals(info.toString(), ResponseInfo.InvalidArgument,
                info.statusCode);
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
            Assert.assertNotNull("timeout", info);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(info.toString(), expectKey, key);
        Assert.assertTrue(info.toString(), info.isOK());
        //上传策略含空格 \"fname\":\" $(fname) \"
        Assert.assertEquals(f.getName(), resp.optString("fname", "res doesn't include the FNAME").trim());
        Assert.assertNotNull(info.reqId);
        Assert.assertNotNull(resp);

        String hash = resp.getString("hash");
        Assert.assertEquals(hash, Etag.file(f));
        TempFile.remove(f);
    }

    @MediumTest
    public void test0File() throws Throwable {
        final String expectKey = "世/界";
        final File f = TempFile.createFile(0);
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
            signal.await(10, TimeUnit.SECONDS); // wait for callback
            Assert.assertNotNull("timeout", info);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(f.toString(), 0, f.length());
        Assert.assertEquals(info.toString(), ResponseInfo.ZeroSizeFile, info.statusCode);
        Assert.assertEquals(info.toString(), expectKey, key);
        Assert.assertFalse(info.toString(), info.isOK());
        Assert.assertEquals(info.toString(), "", info.reqId);
        Assert.assertNull(resp);
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
        ServiceAddress s = new ServiceAddress("http://upwelcome.qiniu.com", Zone.zone0.up.backupIps);
        Zone z = new Zone(s, Zone.zone0.upBackup);
        Configuration c = new Configuration.Builder()
                .zone(z)
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
            Assert.assertNotNull("timeout", info);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(info.toString(), expectKey, key);
        Assert.assertTrue(info.toString(), info.isOK());
        Assert.assertNotNull(info.reqId);
        Assert.assertNotNull(resp);
    }

    @SmallTest
    public void testPortBackup() throws Throwable {
        ServiceAddress s = new ServiceAddress("http://upload.qiniu.com:9999", null);
        Zone z = new Zone(s, Zone.zone0.upBackup);
        Configuration c = new Configuration.Builder()
                .zone(z)
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
            Assert.assertNotNull("timeout", info);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(info.toString(), expectKey, key);
        Assert.assertTrue(info.toString(), info.isOK());
        Assert.assertNotNull(info.reqId);
        Assert.assertNotNull(resp);
    }

    @SmallTest
    public void testDnsHijacking() throws Throwable {
        ServiceAddress s = new ServiceAddress("http://uphijacktest.qiniu.com", Zone.zone0.up.backupIps);
        Zone z = new Zone(s, Zone.zone0.upBackup);
        Configuration c = new Configuration.Builder()
                .zone(z)
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
            Assert.assertNotNull("timeout", info);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(info.toString(), expectKey, key);
        Assert.assertTrue(info.toString(), info.isOK());
        Assert.assertNotNull(info.reqId);
        Assert.assertNotNull(resp);
    }

    @SmallTest
    public void testHttps() throws Throwable {

        final String expectKey = "你好;\"\r\n\r\n\r\n";
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:foo", "fooval");
        final UploadOptions opt = new UploadOptions(params, null, true, null, null);
        ServiceAddress s = new ServiceAddress("https://up.qbox.me", null);
        Zone z = new Zone(s, Zone.zone0.upBackup);
        Configuration c = new Configuration.Builder()
                .zone(z)
                .build();
        UploadManager uploadManager2 = new UploadManager(c);
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
            Assert.assertNotNull("timeout", info);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(info.toString(), expectKey, key);
        Assert.assertTrue(info.toString(), info.isOK());
        Assert.assertNotNull(info.reqId);
        Assert.assertNotNull(resp);
    }
}
