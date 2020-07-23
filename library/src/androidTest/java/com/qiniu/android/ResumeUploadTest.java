package com.qiniu.android;

import android.test.suitebuilder.annotation.LargeTest;

import com.qiniu.android.common.FixedZone;
import com.qiniu.android.common.Zone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.utils.AsyncRun;
import com.qiniu.android.utils.Etag;
import com.qiniu.android.utils.LogUtil;

import org.json.JSONObject;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;


public class ResumeUploadTest extends BaseTest {

    private UploadManager uploadManager;
    private volatile String key;
    private volatile ResponseInfo info = null;
    private volatile JSONObject resp;
    private Queue<Double> queue = new LinkedList<Double>() {{
        offer(0.00000001);
        offer(0.00000001);
        offer(0.00000001);
        offer(0.00000001);
        offer(0.00000001);
    }};

    private void putProgress(double size) {
        if (size > 0.94) {
            return;
        }
        queue.offer(size);
        if (queue.size() > 4) {
            queue.poll();
        }
    }

    private boolean isProgressAllSame() {
        double size = 0.00000001;
        double _lit = 0.0000000001;
        boolean same = true;
        Iterator<Double> it = queue.iterator();
        while (it.hasNext()) {
            double temp = it.next();
            same = same && (Math.abs(size - temp) < _lit);
            size = temp;
        }
        return same;
    }

    private String getProgress() {
        StringBuilder ss = new StringBuilder("");
        Iterator<Double> it = queue.iterator();
        while (it.hasNext()) {
            ss.append(it.next()).append(", ");
        }
        return ss.toString();
    }

    private UploadOptions getUploadOptions() {
        return new UploadOptions(null, null, false, new UpProgressHandler() {
            public void progress(String key, double percent) {
                LogUtil.d("== percent:" + percent);
                putProgress(percent);
            }
        }, null);
    }

    public void setUp() throws Exception {
        Configuration config = new Configuration.Builder()
                .useConcurrentResumeUpload(false)
                .build();
        uploadManager = new UploadManager(config);
    }

    private void template(int size, boolean useHttps) throws Throwable {

        final String expectKey = "android-resume-test2-" + size + "k";
        final File f = TempFile.createFile(size);
        String[] s = new String[]{"up-na0.qbox.me"};
        Zone z = new FixedZone(s);

        Configuration.Builder builder = new Configuration.Builder()
                .zone(z).useConcurrentResumeUpload(false)
                .useHttps(useHttps);
        Configuration configuration = builder.build();
        UploadManager uploadManager = new UploadManager(configuration);
        final UploadOptions options = getUploadOptions();
        uploadManager.put(f, expectKey, TestConfig.commonToken, new UpCompletionHandler() {
            public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                LogUtil.i(k + rinfo);
                key = k;
                info = rinfo;
                resp = response;
            }
        }, options);

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (resp == null){
                    return true;
                } else {
                    return false;
                }
            }
        }, 1200);

        assertTrue(info.toString(), info.isOK());
        assertNotNull(info.reqId);
        assertEquals(info.toString(), expectKey, key);
        String hash = resp.getString("hash");
        assertEquals(hash, Etag.file(f));
        TempFile.remove(f);
    }

    @LargeTest
    public void test4M1K() throws Throwable {
        template(1024 * 4 + 1, false);
    }

    @LargeTest
    public void test8M1L() throws Throwable {
        template(1024 * 8 + 1, false);
    }

    @LargeTest
    public void test20M1K() throws Throwable {
        template(1024 * 20 + 1, false);
    }

    @LargeTest
    public void test4M1kHttps() throws Throwable {
        template(1024 * 4 + 1, true);
    }

    @LargeTest
    public void test8M1kHttps() throws Throwable {
        template(1024 * 8 + 1, true);
    }

    @LargeTest
    public void test20M1kHttps() throws Throwable {
        template(1024 * 20 + 1, true);
    }


    @LargeTest
    public void testError() throws Throwable {

        final String expectKey = "android-resume-error-test2";
        final File f = TempFile.createFile(1);;
        String[] s = new String[]{""};
        Zone z = new FixedZone(s);

        Configuration.Builder builder = new Configuration.Builder()
                .zone(z).useConcurrentResumeUpload(false)
                .useHttps(true);
        Configuration configuration = builder.build();
        UploadManager uploadManager = new UploadManager(configuration);
        final UploadOptions options = getUploadOptions();
        uploadManager.put(f, expectKey, TestConfig.commonToken, new UpCompletionHandler() {
            public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                LogUtil.i(k + rinfo);
                key = k;
                info = rinfo;
                resp = response;
            }
        }, options);

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (info == null){
                    return true;
                } else {
                    return false;
                }
            }
        }, 1200);

        assertTrue(info.toString(), !info.isOK());
        assertNotNull(info.reqId == null);
        assertEquals(info.toString(), expectKey, key);

        TempFile.remove(f);
    }

    @LargeTest
    public void testNoTokenError() throws Throwable {

        final String expectKey = "android-resume-noToken-test2";
        final File f = TempFile.createFile(1);
        String[] s = new String[]{"up-na0.qbox.me"};
        Zone z = new FixedZone(s);

        Configuration.Builder builder = new Configuration.Builder()
                .zone(z).useConcurrentResumeUpload(false)
                .useHttps(true);
        Configuration configuration = builder.build();
        UploadManager uploadManager = new UploadManager(configuration);
        final UploadOptions options = getUploadOptions();

        // file
        uploadManager.put(f, expectKey, null, new UpCompletionHandler() {
            public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                LogUtil.i(k + rinfo);
                key = k;
                info = rinfo;
                resp = response;
            }
        }, options);

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (info == null){
                    return true;
                } else {
                    return false;
                }
            }
        }, 1200);

        assertTrue(info.toString(), !info.isOK());
        assertEquals(info.toString(), expectKey, key);
        TempFile.remove(f);

        // data
        byte[] data = new byte[]{0, 1};
        uploadManager.put(data, expectKey, "1", new UpCompletionHandler() {
            public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                LogUtil.i(k + rinfo);
                key = k;
                info = rinfo;
                resp = response;
            }
        }, options);

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (info == null){
                    return true;
                } else {
                    return false;
                }
            }
        }, 1200);

        assertTrue(info.toString(), !info.isOK());
        assertEquals(info.toString(), expectKey, key);
        TempFile.remove(f);
    }


    @LargeTest
    public void testNoDataError() throws Throwable {

        final String expectKey = "android-resume-noToken-test2";
        String[] s = new String[]{"up-na0.qbox.me"};
        Zone z = new FixedZone(s);

        Configuration.Builder builder = new Configuration.Builder()
                .zone(z).useConcurrentResumeUpload(false)
                .useHttps(true);
        Configuration configuration = builder.build();
        UploadManager uploadManager = new UploadManager(configuration);
        final UploadOptions options = getUploadOptions();

        // file
        File f = null;
        uploadManager.put(f, expectKey, TestConfig.commonToken, new UpCompletionHandler() {
            public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                LogUtil.i(k + rinfo);
                key = k;
                info = rinfo;
                resp = response;
            }
        }, options);

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (info == null){
                    return true;
                } else {
                    return false;
                }
            }
        }, 1200);

        assertTrue(info.toString(), !info.isOK());
        assertEquals(info.toString(), expectKey, key);
        TempFile.remove(f);

        // file path
        String fPath = null;
        uploadManager.put(fPath, expectKey, TestConfig.commonToken, new UpCompletionHandler() {
            public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                LogUtil.i(k + rinfo);
                key = k;
                info = rinfo;
                resp = response;
            }
        }, options);

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (info == null){
                    return true;
                } else {
                    return false;
                }
            }
        }, 1200);

        assertTrue(info.toString(), !info.isOK());
        assertEquals(info.toString(), expectKey, key);
        TempFile.remove(f);

        // data
        byte[] data = null;
        uploadManager.put(data, expectKey, TestConfig.commonToken, new UpCompletionHandler() {
            public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                LogUtil.i(k + rinfo);
                key = k;
                info = rinfo;
                resp = response;
            }
        }, options);

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (info == null){
                    return true;
                } else {
                    return false;
                }
            }
        }, 1200);

        assertTrue(info.toString(), !info.isOK());
        assertEquals(info.toString(), expectKey, key);
    }
}