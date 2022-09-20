package com.qiniu.android.storage;


import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.BaseTest;
import com.qiniu.android.TempFile;
import com.qiniu.android.TestConfig;
import com.qiniu.android.common.FixedZone;
import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.common.ZonesInfo;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.utils.LogUtil;

import junit.framework.Assert;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by Simon on 2015/4/15.
 */
@RunWith(AndroidJUnit4.class)
public class CancelTest extends BaseTest {
    private volatile UploadManager uploadManager;
    private volatile UploadOptions options;

    @Before
    public void setUp() throws Exception {
        File f = File.createTempFile("qiniutest", "b");
        String folder = f.getParent();
        FileRecorder fr = new FileRecorder(folder);
        uploadManager = new UploadManager(fr);
    }

    @Test
    public void testFile() throws Throwable {
        Temp[] ts = new Temp[]{templateFile(8 * 1024 + 1, 0.6)};
        checkTemp(ts, "testFile");
    }

    @Test
    public void testMultiFile() throws Throwable {
        Temp[] ts = new Temp[]{templateFile(400, 0.01), templateFile(700, 0.02), templateFile(1024, 0.02), templateFile(4 * 1024, 0.02), templateFile(8 * 1024 + 1, 0.2)};
        checkTemp(ts, "testFile");
    }

//    public void testData() throws Throwable {
//        Temp[] ts = new Temp[]{templateData(400, 0.2), templateData(700, 0.2), templateData(1024, 0.51), templateData(4 * 1024 + 785, 0.5), templateData(4 * 1024, 0.5), templateData(8 * 1024, 0.6)};
//        checkTemp(ts, "testData");
//    }

    private void checkTemp(Temp[] ts, String type) {
        int failedCount = 0;
        Temp tt = null;

        for (int i = 0; i < ts.length; i++) {
            Temp t = ts[i];
            boolean b = t.expectKey.equals(t.key) && t.info.isCancelled() && (t.resp == null);
            if (!b) {
                tt = t;
                failedCount++;
            }
        }

        LogUtil.d(type + "   " + failedCount);
        if (failedCount > ts.length / 2) {
            String info = type + ": 共 " + ts.length + "个测试，至多允许 " + ts.length / 2 + " 失败，实际失败 " + failedCount + " 个： " + tt.info.toString();
            Assert.assertEquals(info, tt.expectKey, tt.key);
        }
    }


    private Temp templateFile(final int size, final double pos) throws Throwable {
        final CountDownLatch signal = new CountDownLatch(1);
        final File tempFile = TempFile.createFile(size);
        final String expectKey = "file_" + size;
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:a", "test");
        params.put("x:b", "test2");

        final Temp temp = new Temp();
        temp.cancelled = false;

        Thread t = new Thread() {
            public void run() {
                try {
                    Thread.sleep(8 * 60 * 1000);
                    temp.cancelled = true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        t.setDaemon(true);
        t.start();

        final WaitCondition waitCondition = new WaitCondition();

        options = new UploadOptions(params, null, false, new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                if (percent >= pos) {
                    temp.cancelled = true;
                }
                LogUtil.i(pos + ": progress " + percent);
            }
        }, new UpCancellationSignal() {
            @Override
            public boolean isCancelled() {
                return temp.cancelled;
            }
        });

        uploadManager.put(tempFile, expectKey, TestConfig.commonToken, new UpCompletionHandler() {
            public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                temp.expectKey = expectKey;
                temp.key = k;
                temp.info = rinfo;
                temp.resp = response;
                signal.countDown();
                LogUtil.i(k + rinfo);

                waitCondition.shouldWait = false;
            }
        }, options);

        wait(waitCondition, 10 * 60);
        assertTrue(temp.info != null);
        assertTrue(temp.info.statusCode == ResponseInfo.Cancelled);


        waitCondition.shouldWait = true;

        // 断点续传
        options = new UploadOptions(params, null, false, new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                LogUtil.i(pos + ": progress " + percent);
            }
        }, null);
        uploadManager.put(tempFile, expectKey, TestConfig.commonToken, new UpCompletionHandler() {
            public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                temp.expectKey = expectKey;
                temp.key = k;
                temp.info = rinfo;
                temp.resp = response;
                signal.countDown();
                LogUtil.i(k + rinfo);

                waitCondition.shouldWait = false;
            }
        }, options);

        wait(waitCondition, 10 * 60);

        assertTrue(temp.info != null);
        assertTrue(temp.info.isOK());

        TempFile.remove(tempFile);
        return temp;
    }

    private Temp templateData(final int size, final double pos) throws Throwable {
        final CountDownLatch signal = new CountDownLatch(1);
        final byte[] tempDate = TempFile.getByte(1024 * size);
        final String expectKey = "data_" + UUID.randomUUID().toString();
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:a", "test");
        params.put("x:b", "test2");

        final Temp temp = new Temp();
        temp.cancelled = false;

        Thread t = new Thread() {
            public void run() {
                try {
                    Thread.sleep(8 * 60 * 1000);
                    temp.cancelled = true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        t.setDaemon(true);
        t.start();

        options = new UploadOptions(params, null, false, new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                if (percent >= pos) {
                    temp.cancelled = true;
                }
                LogUtil.i(pos + ": progress " + percent);
            }
        }, new UpCancellationSignal() {
            @Override
            public boolean isCancelled() {
                return temp.cancelled;
            }
        });

        uploadManager.put(tempDate, expectKey, TestConfig.commonToken, new UpCompletionHandler() {
            public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                temp.expectKey = expectKey;
                temp.key = k;
                temp.info = rinfo;
                temp.resp = response;
                signal.countDown();
                LogUtil.i(k + rinfo);
            }
        }, options);


        try {
            signal.await(570, TimeUnit.SECONDS); // wait for callback
            Assert.assertNotNull("timeout", temp.info);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return temp;
    }

    @Test
    public void testSwitchRegion() throws IOException {

        final CountDownLatch signal = new CountDownLatch(1);
        final File tempFile = TempFile.createFile(5 * 1024);
        final String expectKey = "file_" + UUID.randomUUID().toString();
        Map<String, String> params = new HashMap<String, String>();
        params.put("x:a", "test");
        params.put("x:b", "test2");

        final Temp temp = new Temp();
        temp.cancelled = false;

        Thread t = new Thread() {
            public void run() {
                try {
                    Thread.sleep(8 * 60 * 1000);
                    temp.cancelled = true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        t.setDaemon(true);
        t.start();

        final WaitCondition waitCondition = new WaitCondition();

        options = new UploadOptions(params, null, false, new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                LogUtil.i("progress " + percent);
            }
        }, null);

        File f = File.createTempFile("qiniutest", "b");
        String folder = f.getParent();
        FileRecorder fr = new FileRecorder(folder);

        ArrayList<String> hostArray0 = new ArrayList<>();
        hostArray0.add("mock1.up.qiniup.com");
        ZoneInfo zoneInfo0 = ZoneInfo.buildInfo(hostArray0, null, null);
        ArrayList<String> hostArray1 = new ArrayList<>();
        hostArray1.add("up-na0.qiniup.com");
        ZoneInfo zoneInfo1 = ZoneInfo.buildInfo(hostArray1, null, null);

        ArrayList<ZoneInfo> zoneInfoArray = new ArrayList<>();
        zoneInfoArray.add(zoneInfo0);
        zoneInfoArray.add(zoneInfo1);
        ZonesInfo zonesInfo = new ZonesInfo(zoneInfoArray);
        FixedZone zone = new FixedZone(zonesInfo);

        Configuration config = new Configuration.Builder().recorder(fr).zone(zone).build();

        UploadManager uploadManager = new UploadManager(config);
        uploadManager.put(tempFile, expectKey, TestConfig.commonToken, new UpCompletionHandler() {
            public void complete(String k, ResponseInfo rinfo, JSONObject response) {
                temp.expectKey = expectKey;
                temp.key = k;
                temp.info = rinfo;
                temp.resp = response;
                signal.countDown();
                LogUtil.i(k + rinfo);

                waitCondition.shouldWait = false;
            }
        }, options);

        wait(waitCondition, 10 * 60);

        assertTrue(temp.info != null);
        assertTrue(temp.info.toString(), temp.info.isOK());

        TempFile.remove(tempFile);
    }

    private static class Temp {
        volatile ResponseInfo info;
        volatile JSONObject resp;
        volatile String key;
        volatile String expectKey;
        volatile boolean cancelled;
    }
}
