package com.qiniu.android.http.request;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.TestConfig;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.request.handler.RequestProgressHandler;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.BaseTest;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.utils.LogUtil;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;


@RunWith(AndroidJUnit4.class)
public class RequestTransactionTest extends BaseTest {

    @Test
    public void testUCQuery(){

        UpToken token = UpToken.parse(TestConfig.token_z0);

        ArrayList<String> hosts = new ArrayList<String>();
        hosts.add("uc.qbox.me");

        final WaitCondition waitCondition = new WaitCondition();

        RequestTransaction requestTransaction = new RequestTransaction(hosts, token);
        requestTransaction.queryUploadHosts(true, new RequestTransaction.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                Assert.assertTrue("pass", responseInfo.isOK());
                waitCondition.shouldWait = false;
            }
        });

        wait(waitCondition, 60);
    }

    @Test
    public void testUploadForm(){

        final WaitCondition waitCondition = new WaitCondition();

        UpToken token = UpToken.parse(TestConfig.token_z0);

        ArrayList<String> hosts = new ArrayList<String>();
        hosts.add("upload.qiniup.com");

        byte[] data = "你好".getBytes();
        RequestTransaction requestTransaction = new RequestTransaction(new Configuration.Builder().build(),
                UploadOptions.defaultOptions(),
                hosts, null,
                "android-transaction-form",
                token);
        requestTransaction.uploadFormData(data, null, true, new RequestProgressHandler() {
            @Override
            public void progress(long totalBytesWritten, long totalBytesExpectedToWrite) {
                LogUtil.i(("== progress: " + (totalBytesWritten*1.0/totalBytesExpectedToWrite)));
            }
        }, new RequestTransaction.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                Assert.assertTrue("pass", responseInfo.isOK());
                waitCondition.shouldWait = false;
            }
        });

        wait(waitCondition, 60);
    }


    @Test
    public void testUploadBlock(){

        final WaitCondition waitCondition = new WaitCondition();

        makeBlock(new RequestTransaction.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {

                if (responseInfo.isOK()){

                    String ct = null;
                    try {
                        ct = response.getString("ctx");
                    } catch (JSONException e) {}

                    if (ct == null) {
                        Assert.assertTrue("pass", false);
                        waitCondition.shouldWait = false;
                        return;
                    }

                    uploadChunk(ct, new RequestTransaction.RequestCompleteHandler(){
                        @Override
                        public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {

                            if (responseInfo.isOK()){

                                String ct_02 = null;
                                try {
                                    ct_02 = response.getString("ctx");
                                } catch (JSONException e) {}

                                if (ct_02 == null) {
                                    Assert.assertTrue("pass", false);
                                    waitCondition.shouldWait = false;
                                    return;
                                }

                                makeFile(new String[]{ct_02}, new RequestTransaction.RequestCompleteHandler() {
                                    @Override
                                    public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                                        Assert.assertTrue("pass", responseInfo.isOK());
                                        waitCondition.shouldWait = false;
                                    }
                                });
                            } else {
                                Assert.assertTrue("pass", false);
                                waitCondition.shouldWait = false;
                            }
                        }
                    });
                } else {
                    Assert.fail("fail");
                    waitCondition.shouldWait = false;
                }
            }
        });

        wait(waitCondition, 60);
    }

    private void makeBlock(RequestTransaction.RequestCompleteHandler completeHandler){
        UpToken token = UpToken.parse(TestConfig.token_z0);

        ArrayList<String> hosts = new ArrayList<String>();
        hosts.add("upload.qiniup.com");

        byte[] data = new byte[2*1024*1024];
        RequestTransaction requestTransaction = new RequestTransaction(new Configuration.Builder().build(), UploadOptions.defaultOptions(), hosts, null, "android-transaction-block", token);
        requestTransaction.makeBlock(0, 3*1024*1024, data, true, new RequestProgressHandler() {
            @Override
            public void progress(long totalBytesWritten, long totalBytesExpectedToWrite) {
                LogUtil.i(("== progress: " + (totalBytesWritten*1.0/totalBytesExpectedToWrite)));
            }
        }, completeHandler);
    }

    private void uploadChunk(String ct, RequestTransaction.RequestCompleteHandler completeHandler){
        UpToken token = UpToken.parse(TestConfig.token_z0);

        ArrayList<String> hosts = new ArrayList<String>();
        hosts.add("upload.qiniup.com");

        byte[] data = new byte[1*1024*1024];
        RequestTransaction requestTransaction = new RequestTransaction(new Configuration.Builder().build(), UploadOptions.defaultOptions(), hosts, null,"android-transaction-block", token);
        requestTransaction.uploadChunk(ct,0, data, 2*1024*1024,true, new RequestProgressHandler() {
            @Override
            public void progress(long totalBytesWritten, long totalBytesExpectedToWrite) {
                LogUtil.i(("== progress: " + (totalBytesWritten*1.0/totalBytesExpectedToWrite)));
            }
        }, completeHandler);
    }

    private void makeFile(String[] blockContexts, RequestTransaction.RequestCompleteHandler completeHandler){
        UpToken token = UpToken.parse(TestConfig.token_z0);

        ArrayList<String> hosts = new ArrayList<String>();
        hosts.add("upload.qiniup.com");

        RequestTransaction requestTransaction = new RequestTransaction(new Configuration.Builder().build(), UploadOptions.defaultOptions(), hosts,null, "android-transaction-block", token);
        requestTransaction.makeFile(3*1024*1024, "android-transaction-block-fileName", blockContexts, true, completeHandler);
    }

    @Test
    public void testMakeFileError(){
        final WaitCondition waitCondition = new WaitCondition();

        makeFile(null, new RequestTransaction.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                Assert.assertTrue("pass", !responseInfo.isOK());
                waitCondition.shouldWait = false;
            }
        });

        wait(waitCondition, 60);
    }
}
