package com.qiniu.android.storage;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.TempFile;
import com.qiniu.android.http.ResponseInfo;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

@RunWith(AndroidJUnit4.class)
public class ComplexUploadSceneTest extends UploadBaseTest {

    private final CountDownLatch signal = new CountDownLatch(1);

    @Test
    public void testMutiUploadV1() {

        final int maxCount = 40;

        final TestParam param = new TestParam();
        param.completeCount = 0;
        param.successCount = 0;

        final int start = 37;
        for (int i = start; i < maxCount; i++) {
            Configuration config = new Configuration.Builder()
                    .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V1)
                    .useConcurrentResumeUpload(true)
                    .concurrentTaskCount(3)
                    .chunkSize((i % 4 + 1) * 1024 * 1024 + i)
                    .build();

            int size = (i + 1) * 1024;
            final String keyUp = "android_complex_upload_v1_" + size + "k";
            File file = null;
            try {
                file = TempFile.createFile(size);
            } catch (IOException e) {
                continue;
            }

            UploadInfo<File> info = new UploadInfo<>(file);
            info.configWithFile(file);
            upload(info, keyUp, config, null, new UpCompletionHandler() {
                @Override
                public void complete(String key, ResponseInfo info, JSONObject response) {
                    synchronized (param) {
                        param.completeCount += 1;
                        if (info != null && info.isOK()) {
                            param.successCount += 1;
                        }
                        if (param.completeCount == (maxCount - start)) {
                            signal.countDown();
                        }
                    }
                    Log.d("upload key:" + keyUp, "complex_upload_v1 response: " + info);
                }
            });
        }

        try {
            signal.await(); // wait for callback
        } catch (InterruptedException e) {
        }

        Log.d("ComplexUploadSceneTest", "complex_upload_v1 successCount: " + param.successCount);
        assertTrue("success count" + param.successCount, param.successCount == param.completeCount);
    }

    @Test
    public void testMutiUploadV2() {

        final int maxCount = 40;

        final TestParam param = new TestParam();
        param.completeCount = 0;
        param.successCount = 0;

        final int start = 37;
        for (int i = start; i < maxCount; i++) {
            Configuration config = new Configuration.Builder()
                    .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V2)
                    .useConcurrentResumeUpload(true)
                    .concurrentTaskCount(3)
                    .chunkSize((i % 4 + 1) * 1024 * 1024 + i)
                    .build();

            int size = (i + 1) * 1024;
            final String keyUp = "android_complex_upload_v2_" + size + "k";
            File file = null;
            try {
                file = TempFile.createFile(size);
            } catch (IOException e) {
                continue;
            }

            UploadInfo<File> info = new UploadInfo<>(file);
            info.configWithFile(file);
            upload(info, keyUp, config, null, new UpCompletionHandler() {
                @Override
                public void complete(String key, ResponseInfo info, JSONObject response) {
                    synchronized (param) {
                        param.completeCount += 1;
                        if (info != null && (info.isOK() || info.statusCode == 614)) {
                            param.successCount += 1;
                        }
                        if (param.completeCount == (maxCount - start)) {
                            signal.countDown();
                        }
                    }
                    Log.d("upload key:" + keyUp, "complex_upload_v2 response: " + info);
                }
            });
        }

        try {
            signal.await(); // wait for callback
        } catch (InterruptedException e) {
        }

        Log.d("ComplexUploadSceneTest", "complex_upload_v2 successCount: " + param.successCount);
        assertTrue("success count" + param.successCount, param.successCount == (param.completeCount));
    }


    private class TestParam {
        int successCount = 0;
        int completeCount = 0;
    }

}
