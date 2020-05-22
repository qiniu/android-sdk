package com.qiniu.android;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.request.Request;
import com.qiniu.android.http.request.RequestClient;
import com.qiniu.android.http.request.httpclient.SystemHttpClient;
import com.qiniu.android.http.metrics.UploadSingleRequestMetrics;



import org.json.JSONObject;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class SystemHttpClientTest extends TestCase {

    final CountDownLatch signal = new CountDownLatch(1);

    public void testGet() {

        Request request = new Request("https://uc.qbox.me/v3/query?ak=jH983zIUFIP1OVumiBVGeAfiLYJvwrF45S-t22eu&bucket=zone0-space",
                null, null, null, 15);

        SystemHttpClient client = new SystemHttpClient();
        client.request(request, true, null, null, new RequestClient.RequestClientCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadSingleRequestMetrics metrics, JSONObject response) {

                Assert.assertTrue("pass", responseInfo.isOK());
                signal.countDown();
            }
        });


        try {
            signal.await(6000, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void testPost() {

        Request request = new Request("http://www.baidu.com/",
                Request.HttpMethodPOST, null, "hello".getBytes(), 15);

        SystemHttpClient client = new SystemHttpClient();
        client.request(request, true, null, null, new RequestClient.RequestClientCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadSingleRequestMetrics metrics, JSONObject response) {

                Assert.assertTrue("pass", responseInfo.isOK());
                signal.countDown();
            }
        });


        try {
            signal.await(6000, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
