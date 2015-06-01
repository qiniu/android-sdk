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

import junit.framework.Assert;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by bailong on 14/10/12.
 */
public class PortTest extends InstrumentationTestCase {
    final CountDownLatch signal = new CountDownLatch(1);
    private UploadManager uploadManager;
    private volatile String key;
    private volatile ResponseInfo info;
    private volatile JSONObject resp;

    @Override
    protected void setUp() throws Exception {
        Configuration config = new Configuration.Builder().upPort(8888).build();
        uploadManager = new UploadManager(config);
    }

    @SmallTest
    public void testData() throws Throwable {
        final String expectKey = "你好;\"\r\n\r\n\r\n_port";
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

        check(expectKey);
    }

    @MediumTest
    public void test123K() throws Throwable {
        fileTemplate(123);
    }

    @MediumTest
    public void test323K() throws Throwable {
        fileTemplate(323);
    }

    @MediumTest
    public void test4223K() throws Throwable {
        fileTemplate(4223);
    }

    public void fileTemplate(int size) throws Throwable {
        final String expectKey = "r=" + size + "k_port";
        final File f = TempFile.createFile(size);
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

        check(expectKey);
    }

    private void check(final String expectKey) {
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
