package com.qiniu.android;

import com.qiniu.android.bigdata.client.Client;
import com.qiniu.android.bigdata.client.CompletionHandler;
import com.qiniu.android.bigdata.client.PostArgs;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.request.httpclient.MultipartBody;
import com.qiniu.android.storage.UpToken;

import org.json.JSONObject;

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

        UpToken token = UpToken.parse(TestConfig.commonToken);

        Client client = new Client(null, 90, 90, null, null);
        ResponseInfo responseInfo = client.syncMultipartPost("https://up.qiniu.com", postArgs, token);

        assertTrue(responseInfo != null);
        assertTrue(responseInfo.statusCode == 200);
    }
}
