package com.qiniu.android;

import android.test.AndroidTestCase;
import android.util.Log;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;

import junit.framework.Assert;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ComplexUploadSceneTest extends AndroidTestCase {

    private final CountDownLatch signal = new CountDownLatch(1);

    public void testMutiUpload(){

        final int maxCount = 10;

        final TestParam param = new TestParam();
        param.completeCount = 0;
        param.successCount = 0;

        for (int i = 0; i < maxCount; i++) {
            template((i + 1) * 100, new CompleteHandler() {
                @Override
                public void complete(boolean isSuccess) {

                    synchronized (param){
                        param.completeCount += 1;
                        if (isSuccess){
                            param.successCount += 1;
                        }
                        if (param.completeCount == maxCount){
                            signal.countDown();
                        }
                    }
                }
            });
        }

        try {
            signal.await(); // wait for callback
        } catch (InterruptedException e) {
        }

        Log.d("ComplexUploadSceneTest", "complex_upload successCount: " + param.successCount);
        assertTrue("success count" + param.successCount, param.successCount == param.completeCount);
    }

    private void template(int size, final CompleteHandler completeHandler){

        final String keyUp = "android_complex_upload_" + size + "k";
        File file = null;
        try {
            file = TempFile.createFile(size);
        } catch (IOException e) {
            completeHandler.complete(false);
            return;
        }

        Configuration configuration = new Configuration.Builder()
                .useHttps(true)
                .build();
        UploadManager manager = new UploadManager(configuration);

        manager.put(file, keyUp, TestConfig.token_na0, new UpCompletionHandler() {
            @Override
            public void complete(String key, ResponseInfo info, JSONObject response) {
                if (info.isOK() && info.reqId != null && keyUp.equals(key)){
                    completeHandler.complete(true);
                } else {
                    completeHandler.complete(false);
                }
            }
        }, null);
    }

    private interface CompleteHandler{
        void complete(boolean isSuccess);
    }


    private class TestParam{
        int successCount = 0;
        int completeCount = 0;
    }

}
