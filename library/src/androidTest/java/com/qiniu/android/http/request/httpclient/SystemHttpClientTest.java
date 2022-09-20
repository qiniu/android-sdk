package com.qiniu.android.http.request.httpclient;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.request.Request;
import com.qiniu.android.http.request.IRequestClient;
import com.qiniu.android.http.metrics.UploadSingleRequestMetrics;
import com.qiniu.android.BaseTest;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SystemHttpClientTest extends BaseTest {

    @Test
    public void testGet() {

        final WaitCondition waitCondition = new WaitCondition();

        Request request = new Request("https://uc.qbox.me/v3/query?ak=jH983zIUFIP1OVumiBVGeAfiLYJvwrF45S-t22eu&bucket=zone0-space",
                null, null, null, 15);

        SystemHttpClient client = new SystemHttpClient();
        client.request(request, true, null, null, new IRequestClient.CompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadSingleRequestMetrics metrics, JSONObject response) {
                assertTrue("pass", responseInfo.isOK());
                waitCondition.shouldWait = false;
            }
        });

        wait(waitCondition, 60);
    }

    @Test
    public void testSyncGet() {

        final WaitCondition waitCondition = new WaitCondition();

        Request request = new Request("https://uc.qbox.me/v3/query?ak=jH983zIUFIP1OVumiBVGeAfiLYJvwrF45S-t22eu&bucket=zone0-space",
                null, null, null, 15);

        SystemHttpClient client = new SystemHttpClient();
        client.request(request, false, null, null, new IRequestClient.CompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadSingleRequestMetrics metrics, JSONObject response) {
                assertTrue("pass", responseInfo.isOK());
                waitCondition.shouldWait = false;
            }
        });

        wait(waitCondition, 60);
    }

    @Test
    public void testPostRequest() {

        final WaitCondition waitCondition = new WaitCondition();

        Request request = new Request("http://www.baidu.com/",
                Request.HttpMethodPOST, null, "hello".getBytes(), 15);

        SystemHttpClient client = new SystemHttpClient();
        client.request(request, true, null, null, new IRequestClient.CompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadSingleRequestMetrics metrics, JSONObject response) {

                assertFalse("pass", responseInfo.isServerError());
                waitCondition.shouldWait = false;
            }
        });

        wait(waitCondition, 60);
    }

    @Test
    public void testSyncPostRequest() {

        final WaitCondition waitCondition = new WaitCondition();

        Request request = new Request("http://www.baidu.com/",
                Request.HttpMethodPOST, null, "hello".getBytes(), 15);

        SystemHttpClient client = new SystemHttpClient();
        client.request(request, false, null, null, new IRequestClient.CompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadSingleRequestMetrics metrics, JSONObject response) {

                assertFalse("pass", responseInfo.isServerError());
                waitCondition.shouldWait = false;
            }
        });

        wait(waitCondition, 60);
    }

    @Test
    public void testPostRequestError() {

        final WaitCondition waitCondition = new WaitCondition();

        Request request = new Request("http://www.baidu.com/",
                Request.HttpMethodPOST, null, "hello".getBytes(), 15);

        SystemHttpClient client = new SystemHttpClient();
        client.request(null, true, null, null, new IRequestClient.CompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadSingleRequestMetrics metrics, JSONObject response) {

                assertTrue("pass", responseInfo.statusCode == ResponseInfo.InvalidArgument);
                waitCondition.shouldWait = false;
            }
        });

        wait(waitCondition, 60);
    }
}
