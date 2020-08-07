package com.qiniu.android;

import com.qiniu.android.common.FixedZone;
import com.qiniu.android.common.Zone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.utils.Etag;
import com.qiniu.android.utils.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

/**
 * Created by yangsen on 2020/6/3
 */
public class RetryTest extends BaseTest {


    public void testUpload() {

        final TestParam param = new TestParam();
        param.count = 10;

        for (int i = 0; i < param.count; i++) {

            final int i_p = i;

            new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                        template((i_p + 1), new CompleteHandler() {
                            @Override
                            public void complete(boolean isSuccess) {

                                synchronized (this){
                                    if (isSuccess){
                                        param.successCount += 1;
                                    }
                                    param.completeCount += 1;
                                }

                            }
                        });
                    } catch (Throwable ignored) {
                    }

                }
            }).start();

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (param.completeCount != param.count){
                    return true;
                } else {
                    return false;
                }
            }
        }, 600 * 10);

        assertTrue((param.completeCount == param.successCount));
    }

    private void template(int size, final CompleteHandler completeHandler) throws Throwable{

        final WaitCondition waitCondition = new WaitCondition();

        final String expectKey = "android-retry-" + size + "k";
        final File f = TempFile.createFile(size);
        String[] s = new String[]{"uptemp01.qbox.me", "uptemp02.qbox.me",
                "uptemp03.qbox.me", "uptemp04.qbox.me",
                "uptemp05.qbox.me", "uptemp06.qbox.me",
                "uptemp07.qbox.me", "uptemp08.qbox.me",
                "uptemp09.qbox.me", "uptemp10.qbox.me",
                "uptemp11.qbox.me", "uptemp12.qbox.me",
                "uptemp13.qbox.me", "uptemp14.qbox.me",
                "upload.qiniup.com"};
        Zone z = new FixedZone(s);
        Configuration c = new Configuration.Builder()
                .zone(z).useHttps(true).useConcurrentResumeUpload(false)
                .build();
        UploadManager uploadManager = new UploadManager(c);
        final UploadOptions options = new UploadOptions(null, null, true, new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                LogUtil.i(("progress:" + percent));
            }
        }, null);
        uploadManager.put(f, expectKey, TestConfig.token_z0, new UpCompletionHandler() {

            public void complete(String k, ResponseInfo rinfo, JSONObject response) {

                if (rinfo.isOK() && rinfo.reqId.length() > 0
                        && expectKey.equals(k)){

                    try {
                        String etag_file = Etag.file(f);
                        String etag_server = response.getString("hash");
                        if (etag_file.equals(etag_server)){
                            completeHandler.complete(true);
                        } else {
                            completeHandler.complete(false);
                        }
                    } catch (IOException ignored) {
                    } catch (JSONException ignored) {
                    }
                } else {
                    completeHandler.complete(false);
                }

                waitCondition.shouldWait = false;
            }
        }, options);

        wait(waitCondition, 60);
    }

    private static class TestParam{
        int count = 100;
        int successCount = 0;
        int completeCount = 0;
    }

    private interface CompleteHandler{
        void complete(boolean isSuccess);
    }

}
