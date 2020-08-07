package com.qiniu.android;

import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import com.qiniu.android.common.FixedZone;
import com.qiniu.android.common.Zone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.utils.Etag;

import junit.framework.Assert;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SyncFormUploadTest extends BaseTest {
    private UploadManager uploadManager;
    private volatile String key;
    private volatile ResponseInfo info;
    private volatile JSONObject resp;

    public void setUp() {
        uploadManager = new UploadManager();
    }

    @SmallTest
    public void testHello() {
        final String expectKey = "你好-testHello;\"\r\n\r\n\r\n";
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:foo", "fooval");
        final UploadOptions opt = new UploadOptions(params, null, true, null, null);
        byte[] b = "hello".getBytes();
        info = uploadManager.syncPut(b, expectKey, TestConfig.commonToken, opt);
        resp = info.response;

        Assert.assertTrue(info.toString(), info.isOK());
        Assert.assertNotNull(info.reqId);
        Assert.assertNotNull(resp);

        String hash = resp.optString("hash");
        Assert.assertEquals(hash, Etag.data(b));
        Assert.assertEquals(expectKey, key = resp.optString("key"));
    }

    @SmallTest
    public void test0Data(){
        final String expectKey = "你好-test0Data;\"\r\n\r\n\r\n";
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:foo", "fooval");
        final UploadOptions opt = new UploadOptions(params, null, true, null, null);

        info = uploadManager.syncPut("".getBytes(), expectKey, TestConfig.commonToken, opt);
        resp = info.response;

        assertEquals(info.toString(), ResponseInfo.ZeroSizeFile, info.statusCode);
        assertNull(resp);
    }

    @SmallTest
    public void testNoKey() {
        final String expectKey = null;
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:foo", "fooval");
        final UploadOptions opt = new UploadOptions(params, null, true, null, null);
        info = uploadManager.syncPut("hello".getBytes(), expectKey, TestConfig.commonToken, opt);

        resp = info.response;
        key = resp.optString("key");
        Assert.assertTrue(info.toString(), info.isOK());

        Assert.assertNotNull(info.reqId);
        Assert.assertNotNull(resp);
        Assert.assertEquals("Fqr0xh3cxeii2r7eDztILNmuqUNN", resp.optString("key", ""));
    }

    @SmallTest
    public void testInvalidtoken_z0() {
        final String expectKey = "你好-testInvalidtoken_z0";
        info = uploadManager.syncPut("hello".getBytes(), expectKey, "invalid", null);

        resp = info.response;
        Assert.assertEquals(info.toString(), ResponseInfo.InvalidToken, info.statusCode);
        assertNull(resp);
    }

    @SmallTest
    public void testNoData() {
        final String expectKey = "你好-testNoData";

        info = uploadManager.syncPut((byte[]) null, expectKey, "invalid", null);

        resp = info.response;
        Assert.assertEquals(info.toString(), ResponseInfo.ZeroSizeFile,
                info.statusCode);
        Assert.assertNull(resp);
    }

    @SmallTest
    public void testNotoken_z0() {
        final String expectKey = "你好-testNotoken_z0";
        info = uploadManager.syncPut(new byte[1], expectKey, null, null);

        resp = info.response;
        Assert.assertEquals(info.toString(), ResponseInfo.InvalidToken, info.statusCode);
        Assert.assertNull(resp);
    }

    @SmallTest
    public void testEmptytoken_z0() {
        final String expectKey = "你好-testEmptytoken_z0";
        info = uploadManager.syncPut(new byte[1], expectKey, "", null);

        resp = info.response;
        Assert.assertEquals(info.toString(), ResponseInfo.InvalidToken,
                info.statusCode);
        Assert.assertNull(resp);
    }

    @MediumTest
    public void testFile() throws Throwable {
        final String expectKey = "世/界-testFile";
        final File f = TempFile.createFile(1);
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:foo", "fooval");
        final UploadOptions opt = new UploadOptions(params, null, true, null, null);
        info = uploadManager.syncPut(f, expectKey, TestConfig.commonToken, opt);

        resp = info.response;
        key = resp.optString("key");
        assertEquals(info.toString(), expectKey, key);
        Assert.assertTrue(info.toString(), info.isOK());

        //上传策略含空格 \"fname\":\" $(fname) \"
//        assertEquals(f.getName(), resp.optString("fname", "res doesn't include the FNAME").trim());
        assertNotNull(info.reqId);
        assertNotNull(resp);
        String hash = resp.getString("hash");
        assertEquals(hash, Etag.file(f));
        TempFile.remove(f);
    }

    @MediumTest
    public void test0File() throws Throwable {
        final String expectKey = "世/界-test0File";
        final File f = TempFile.createFile(0);
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:foo", "fooval");
        final UploadOptions opt = new UploadOptions(params, null, true, null, null);
        info = uploadManager.syncPut(f, expectKey, TestConfig.commonToken, opt);

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
        info = uploadManager.syncPut(new byte[0], null, TestConfig.commonToken, null);
        Assert.assertEquals(info.toString(), ResponseInfo.ZeroSizeFile, info.statusCode);

        info = uploadManager.syncPut("", null, TestConfig.commonToken, null);
        Assert.assertEquals(info.toString(), ResponseInfo.ZeroSizeFile, info.statusCode);
    }


    @SmallTest
    public void testHttps() {
        final String expectKey = "你好-testHttps;\"\r\n\r\n\r\n";
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:foo", "fooval");
        final UploadOptions opt = new UploadOptions(params, null, true, null, null);
        String[] s = new String[]{"up-na0.qbox.me"};
        Zone z = new FixedZone(s);
        Configuration c = new Configuration.Builder()
                .zone(z)
                .useHttps(true)
                .build();
        UploadManager uploadManager2 = new UploadManager(c);
        info = uploadManager2.syncPut("hello".getBytes(), expectKey, TestConfig.commonToken, opt);

        resp = info.response;
        key = resp.optString("key");
        Assert.assertEquals(info.toString(), expectKey, key);
        Assert.assertTrue(info.toString(), info.isOK());
        Assert.assertNotNull(info.reqId);
        Assert.assertNotNull(resp);
    }
}
