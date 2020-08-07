package com.qiniu.android;

import com.qiniu.android.bigdata.client.Client;
import com.qiniu.android.bigdata.client.CompletionHandler;
import com.qiniu.android.bigdata.client.PostArgs;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.request.httpclient.MultipartBody;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.utils.StringMap;

import org.json.JSONObject;

import java.util.HashMap;

import okhttp3.MediaType;

public class ClientTest extends BaseTest {

    public void testSyncGet(){

        Client client = new Client(null, 90, 90, null, null);
        ResponseInfo responseInfo = client.syncGet("https://up.qiniup.com/crossdomain.xml", null);
        assertTrue(responseInfo != null);
        assertTrue(responseInfo.statusCode == 200);
    }

    public void testAsyncGet(){

        final WaitCondition waitCondition = new WaitCondition();
        Client client = new Client();
        client.asyncGet("https://up.qiniup.com/crossdomain.xml", null, null, new CompletionHandler() {
            @Override
            public void complete(ResponseInfo info, JSONObject response) {

                assertTrue(info != null);
                assertTrue(info.statusCode == 200);
                waitCondition.shouldWait = false;
            }
        });

        wait(waitCondition, 10 * 60);
    }

    public void testMultipartSyncPost(){

        PostArgs postArgs = new PostArgs();
        postArgs.data = "123".getBytes();
        postArgs.mimeType = "text/plain";
        postArgs.params = new StringMap();
        postArgs.params.put("x:foo", "foo");

        UpToken token = UpToken.parse(TestConfig.commonToken);

        Client client = new Client(null, 90, 90, null, null);
        ResponseInfo responseInfo = client.syncMultipartPost("http://up.qiniu.com", postArgs, token);

        assertTrue(responseInfo != null);
    }

    public void testMultipartAsyncPost(){

        final WaitCondition waitCondition = new WaitCondition();

        PostArgs postArgs = new PostArgs();
        postArgs.data = "123".getBytes();
        postArgs.mimeType = "text/plain";
        postArgs.params = new StringMap();

        UpToken token = UpToken.parse(TestConfig.commonToken);

        Client client = new Client(null, 90, 90, null, null);
        client.asyncMultipartPost("http://up.qiniu.com", postArgs, token, null, new CompletionHandler() {
            @Override
            public void complete(ResponseInfo info, JSONObject response) {

                assertTrue(info != null);
                waitCondition.shouldWait = false;
            }
        }, null);

        wait(waitCondition, 10 * 60);
    }
}
