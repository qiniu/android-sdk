package com.qiniu.android.bigdata.client;


import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.BaseTest;
import com.qiniu.android.TestConfig;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.utils.StringMap;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ClientTest extends BaseTest {

    @Test
    public void testSyncGet() {

        Client client = new Client(null, 90, 90, null, null);
        ResponseInfo responseInfo = client.syncGet("https://up.qiniup.com/crossdomain.xml", null);
        assertTrue(responseInfo != null);
        assertTrue(responseInfo.statusCode == 200);
    }

    @Test
    public void testAsyncGet() {

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

    @Test
    public void testMultipartSyncPost() {

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

    @Test
    public void testMultipartAsyncPost() {

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
