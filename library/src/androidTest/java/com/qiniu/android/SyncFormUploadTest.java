package com.qiniu.android;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.qiniu.android.common.FixedZone;
import com.qiniu.android.common.Zone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.FormUploaderV2;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;

import junit.framework.Assert;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SyncFormUploadTest extends InstrumentationTestCase {
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
        info = uploadManager.syncPut(b, expectKey, TestConfig.token_z0, opt);
        resp = info.response;

//        Assert.assertTrue(info.toString(), info.isOK());
//        Assert.assertNotNull(info.reqId);
//        Assert.assertNotNull(resp);

//        String hash = resp.optString("hash");
//        Assert.assertEquals(hash, Etag.data(b));
//        Assert.assertEquals(expectKey, key = resp.optString("key"));
    }

    @SmallTest
    public void test0Data() throws Throwable {
        final String expectKey = "你好;\"\r\n\r\n\r\n";
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:foo", "fooval");
        final UploadOptions opt = new UploadOptions(params, null, true, null, null);

        info = uploadManager.syncPut("".getBytes(), expectKey, TestConfig.token_z0, opt);
        resp = info.response;

//        key = resp.optString("key");
        Assert.assertEquals(info.toString(), ResponseInfo.ZeroSizeFile, info.statusCode);
//        Assert.assertEquals(info.toString(), expectKey, key);
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
        info = uploadManager.syncPut("hello".getBytes(), expectKey, TestConfig.token_z0, opt);

        resp = info.response;
//        key = resp.optString("key");
//        Assert.assertTrue(info.toString(), info.isOK());

//        Assert.assertNotNull(info.reqId);
//        Assert.assertNotNull(resp);
//        Assert.assertEquals("Fqr0xh3cxeii2r7eDztILNmuqUNN", resp.optString("key", ""));
    }

    @SmallTest
    public void testInvalidtoken_z0() throws Throwable {
        final String expectKey = "你好";
        info = uploadManager.syncPut("hello".getBytes(), expectKey, "invalid", null);

        resp = info.response;
//        key = resp.optString("key");
//        Assert.assertEquals(info.toString(), expectKey, key);
        Assert.assertEquals(info.toString(), ResponseInfo.InvalidToken, info.statusCode);
        Assert.assertNotNull(info.reqId);
        Assert.assertNull(resp);
    }

    @SmallTest
    public void testNoData() throws Throwable {
        final String expectKey = "你好";

        info = uploadManager.syncPut((byte[]) null, expectKey, "invalid", null);

        resp = info.response;
        Assert.assertEquals(info.toString(), ResponseInfo.InvalidArgument,
                info.statusCode);
        Assert.assertNull(resp);
    }

    @SmallTest
    public void testNotoken_z0() throws Throwable {
        final String expectKey = "你好";
        info = uploadManager.syncPut(new byte[1], expectKey, null, null);

        resp = info.response;
        Assert.assertEquals(info.toString(), ResponseInfo.InvalidArgument, info.statusCode);
        Assert.assertNull(resp);
    }

    @SmallTest
    public void testEmptytoken_z0() throws Throwable {
        final String expectKey = "你好";
        info = uploadManager.syncPut(new byte[1], expectKey, "", null);

        resp = info.response;
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
        info = uploadManager.syncPut(f, expectKey, TestConfig.token_z0, opt);

        resp = info.response;
//        key = resp.optString("key");
//        Assert.assertEquals(info.toString(), expectKey, key);
//        Assert.assertTrue(info.toString(), info.isOK());
        //上传策略含空格 \"fname\":\" $(fname) \"
//        Assert.assertEquals(f.getName(), resp.optString("fname", "res doesn't include the FNAME").trim());
        Assert.assertNotNull(info.reqId);
//        Assert.assertNotNull(resp);
//        String hash = resp.getString("hash");
//        Assert.assertEquals(hash, Etag.file(f));
        TempFile.remove(f);
    }

    @MediumTest
    public void test0File() throws Throwable {
        final String expectKey = "世/界";
        final File f = TempFile.createFile(0);
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:foo", "fooval");
        final UploadOptions opt = new UploadOptions(params, null, true, null, null);
        info = uploadManager.syncPut(f, expectKey, TestConfig.token_z0, opt);

        resp = info.response;
        Assert.assertEquals(f.toString(), 0, f.length());
        Assert.assertEquals(info.toString(), ResponseInfo.ZeroSizeFile, info.statusCode);
        Assert.assertNull(resp);
        Assert.assertFalse(info.toString(), info.isOK());
        Assert.assertEquals(info.toString(), "", info.reqId);
        TempFile.remove(f);
    }

    @SmallTest
    public void test0byte() {
        info = uploadManager.syncPut(new byte[0], null, TestConfig.token_z0, null);
        Assert.assertEquals(info.toString(), ResponseInfo.ZeroSizeFile, info.statusCode);

        info = uploadManager.syncPut("", null, TestConfig.token_z0, null);
        Assert.assertEquals(info.toString(), ResponseInfo.ZeroSizeFile, info.statusCode);
    }


    @SmallTest
    public void testHttps() throws Throwable {
        final String expectKey = "你好;\"\r\n\r\n\r\n";
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:foo", "fooval");
        final UploadOptions opt = new UploadOptions(params, null, true, null, null);
        String[] s = new String[]{"up.qbox.me"};
        Zone z = new FixedZone(s);
        Configuration c = new Configuration.Builder()
                .zone(z)
                .useHttps(true)
                .build();
        UploadManager uploadManager2 = new UploadManager(c);
        info = uploadManager2.syncPut("hello".getBytes(), expectKey, TestConfig.token_z0, opt);

        resp = info.response;
//        key = resp.optString("key");
//        Assert.assertEquals(info.toString(), expectKey, key);
//        Assert.assertTrue(info.toString(), info.isOK());
//        Assert.assertNotNull(info.reqId);
//        Assert.assertNotNull(resp);
    }

    //reTest use FormuploadV2
    @SmallTest
    public void testFormUploadSync() throws Throwable {
        final String expectKey = "你好";
        final File f = TempFile.createFile(1);
        info = uploadManager.syncPut(f, expectKey, TestConfig.uptoken_v3_query, null);
        resp = info.response;
        Log.e("qiniutest", "response" + resp);
    }
}
