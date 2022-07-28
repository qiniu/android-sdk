package com.qiniu.android.storage;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.BaseTest;
import com.qiniu.android.TempFile;
import com.qiniu.android.TestConfig;
import com.qiniu.android.common.FixedZone;
import com.qiniu.android.common.Zone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.utils.Etag;

import junit.framework.Assert;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class SyncFormUploadTest extends BaseTest {
    private UploadManager uploadManager;
    private volatile String key;
    private volatile ResponseInfo info;
    private volatile JSONObject resp;

    @Before
    public void setUp() {
        uploadManager = new UploadManager();
    }

    @Test
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

    @Test
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

    @Test
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

    @Test
    public void testInvalidtoken_z0() {
        final String expectKey = "你好-testInvalidtoken_z0";
        info = uploadManager.syncPut("hello".getBytes(), expectKey, "invalid", null);

        resp = info.response;
        Assert.assertEquals(info.toString(), ResponseInfo.InvalidToken, info.statusCode);
        assertNull(resp);
    }

    @Test
    public void testNoData() {
        final String expectKey = "你好-testNoData";

        info = uploadManager.syncPut((byte[]) null, expectKey, "invalid", null);

        resp = info.response;
        Assert.assertEquals(info.toString(), ResponseInfo.ZeroSizeFile,
                info.statusCode);
        Assert.assertNull(resp);
    }

    @Test
    public void testNotoken_z0() {
        final String expectKey = "你好-testNotoken_z0";
        info = uploadManager.syncPut(new byte[1], expectKey, null, null);

        resp = info.response;
        Assert.assertEquals(info.toString(), ResponseInfo.InvalidToken, info.statusCode);
        Assert.assertNull(resp);
    }

    @Test
    public void testEmptytoken_z0() {
        final String expectKey = "你好-testEmptytoken_z0";
        info = uploadManager.syncPut(new byte[1], expectKey, "", null);

        resp = info.response;
        Assert.assertEquals(info.toString(), ResponseInfo.InvalidToken,
                info.statusCode);
        Assert.assertNull(resp);
    }

    @Test
    public void testFile() throws Throwable {
        final String expectKey = "世/界-testFile";
        final File f = TempFile.createFile(5*1024);
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
//        String hash = resp.getString("hash");
//        assertEquals(hash, Etag.file(f));
        TempFile.remove(f);
    }

    @Test
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

    @Test
    public void test0byte() {
        info = uploadManager.syncPut(new byte[0], null, TestConfig.commonToken, null);
        Assert.assertEquals(info.toString(), ResponseInfo.ZeroSizeFile, info.statusCode);

        info = uploadManager.syncPut("", null, TestConfig.commonToken, null);
        Assert.assertEquals(info.toString(), ResponseInfo.ZeroSizeFile, info.statusCode);
    }


    @Test
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
