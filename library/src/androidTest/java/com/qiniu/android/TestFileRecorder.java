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
import com.qiniu.android.utils.Etag;

import junit.framework.Assert;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Date;
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

    // copy from FileRecorder.
    private static String hash(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(base.getBytes());
            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < hash.length; i++) {
                hexString.append(Integer.toString((hash[i] & 0xff) + 0x100, 16).substring(1));
            }
            return hexString.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    protected void setUp() throws Exception {
        File f = File.createTempFile("qiniutest", "b");
        String folder = f.getParent();
        FileRecorder fr = new FileRecorder(folder);
        config = new Configuration.Builder().recorder(fr).build();
        uploadManager = new UploadManager(config);

        ACollectUploadInfoTest.testInit();
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
                uploadManager.put(tempFile, expectKey, TestConfig.token_z0, new UpCompletionHandler() {
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
            Assert.assertNotNull("timeout", info);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(info.toString(), expectKey, key);
//        Assert.assertTrue(info.toString(), info.isCancelled());
//        Assert.assertNull(resp);

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
                uploadManager.put(tempFile, expectKey, TestConfig.token_z0, new UpCompletionHandler() {
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
            signal2.await(1200, TimeUnit.SECONDS); // wait for callback
            Assert.assertNotNull("timeout", info);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(info.toString(), expectKey, key);
//        Assert.assertTrue(info.toString(), info.isOK());
        Assert.assertTrue(!failed);
//        Assert.assertNotNull(resp);

//        String hash = resp.getString("hash");
//        Assert.assertEquals(hash, Etag.file(tempFile));
        TempFile.remove(tempFile);

        ACollectUploadInfoTest.recordFileTest();
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
        template(4 * 1024, 0.6);
    }

    public void test8M1K() throws Throwable {
        template(8 * 1024 + 1, 0.8);
    }

    public void testLastModify() throws IOException {
        File f = File.createTempFile("qiniutest", "b");
        String folder = f.getParent();
        FileRecorder fr = new FileRecorder(folder);

        String key = "test_profile_";
        byte[] data = new byte[3];
        data[0] = 'a';
        data[1] = '8';
        data[2] = 'b';

        fr.set(key, data);
        byte[] data2 = fr.get(key);

        File recoderFile = new File(folder, hash(key));

        long m1 = recoderFile.lastModified();

        assertEquals(3, data2.length);
        assertEquals('8', data2[1]);

        recoderFile.setLastModified(new Date().getTime() - 1000 * 3600 * 48 + 2300);
        data2 = fr.get(key);
        assertEquals(3, data2.length);
        assertEquals('8', data2[1]);

        // 让记录文件过期，两天
        recoderFile.setLastModified(new Date().getTime() - 1000 * 3600 * 48 - 2300);

        long m2 = recoderFile.lastModified();

        // 过期后，记录数据作废
        byte[] data3 = fr.get(key);

        assertNull(data3);
        assertTrue(m1 - m2 > 1000 * 3600 * 48 && m1 - m2 < 1000 * 3600 * 48 + 5500);

        try {
            Thread.sleep(2300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        fr.set(key, data);
        long m4 = recoderFile.lastModified();
        assertTrue(m4 > m1);
    }
}
