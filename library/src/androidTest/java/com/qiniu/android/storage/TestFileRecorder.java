package com.qiniu.android.storage;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.BaseTest;
import com.qiniu.android.TempFile;
import com.qiniu.android.TestConfig;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.utils.LogUtil;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class TestFileRecorder extends BaseTest {
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

    @Override @Before
    public void setUp() throws Exception {
        File f = File.createTempFile("qiniutest", "b");
        String folder = f.getParent();
        FileRecorder fr = new FileRecorder(folder);
        config = new Configuration.Builder().connectTimeout(15000)/*.recorder(fr)*/.build();
        uploadManager = new UploadManager(config);
    }

    private void template(final int size, final double pos) throws Throwable {

        info = null;

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
                LogUtil.i("progress " + percent);
            }
        }, new UpCancellationSignal() {
            @Override
            public boolean isCancelled() {
                return cancelled;
            }
        });

        uploadManager.put(tempFile, expectKey, TestConfig.commonToken, new UpCompletionHandler() {
            public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                LogUtil.i("Cancel:" + k + rinfo);
                key = k;
                info = rinfo;
                resp = response;
            }
        }, options);

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (info == null) {
                    return true;
                } else {
                    return false;
                }
            }
        }, 600);

        assertEquals(info.toString(), expectKey, key);
        assertTrue(info.toString(), info.isCancelled());
        assertNull(resp);

        info = null;
        cancelled = false;
        options = new UploadOptions(null, null, false, new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                if (percent < pos - config.chunkSize / (size * 1024.0)) {
                    failed = true;
                }
                LogUtil.i("continue progress " + percent);
            }
        }, null);


        uploadManager.put(tempFile, expectKey, TestConfig.commonToken, new UpCompletionHandler() {
            public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                LogUtil.i("Continue" + k + rinfo);
                key = k;
                info = rinfo;
                resp = response;
            }
        }, options);

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (info == null) {
                    return true;
                } else {
                    return false;
                }
            }
        }, 60);

        assertEquals(info.toString(), expectKey, key);
        assertTrue(info.toString(), info.isOK());
        assertNotNull(resp);

//        String hash = resp.getString("hash");
//        assertEquals(hash, Etag.file(tempFile));

        TempFile.remove(tempFile);
    }

    @Test
    public void test_4M1K() throws Throwable {
        template(4 * 1024 + 1, 0.5);

    }

    @Test
    public void test5M() throws Throwable {
        template(5 * 1024, 0.51);
    }

    @Test
    public void test_5M1K() throws Throwable {
        template(5 * 1024 + 1, 0.5);

    }

    @Test
    public void test8M() throws Throwable {
        template(8 * 1024, 0.51);
    }

    @Test
    public void test_8M1K() throws Throwable {
        template(8 * 1024 + 1, 0.5);

    }

    @Test
    public void test10M() throws Throwable {
        template(10 * 1024, 0.51);
    }

    @Test
    public void test_10M1K() throws Throwable {
        template(10 * 1024 + 1, 0.5);

    }

    @Test
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

        File recorderFile = new File(folder, hash(key));

        long m1 = recorderFile.lastModified();

        assertEquals(3, data2.length);
        assertEquals('8', data2[1]);

        recorderFile.setLastModified(new Date().getTime() - 1000 * 3600 * 48 + 2300);
        data2 = fr.get(key);
        assertEquals(3, data2.length);
        assertEquals('8', data2[1]);

        // 让记录文件过期，两天
        recorderFile.setLastModified(new Date().getTime() - 1000 * 3600 * 48 - 2300);

        long m2 = recorderFile.lastModified();

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
        long m4 = recorderFile.lastModified();
        assertTrue(m4 > m1);
    }
}
