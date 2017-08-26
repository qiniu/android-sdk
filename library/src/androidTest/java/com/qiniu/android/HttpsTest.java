package com.qiniu.android;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.qiniu.android.http.Client;
import com.qiniu.android.http.CompletionHandler;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpToken;

import junit.framework.Assert;

import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by bailong on 15/12/1.
 */
public class HttpsTest extends InstrumentationTestCase {
    final CountDownLatch signal = new CountDownLatch(1);
    private Client httpManager;
    private ResponseInfo info;

    private static URI newURI(String s) {
        try {
            return new URI(s);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void setUp() throws Exception {
        httpManager = new Client();
    }

    @SmallTest
    public void testPost1() throws Throwable {
        httpManager.asyncPost("https://www.baidu.com/", "hello".getBytes(), null,
                UpToken.parse(TestConfig.token_z0), "hello".getBytes().length, null, new CompletionHandler() {
                    @Override
                    public void complete(ResponseInfo rinfo, JSONObject response) {
                        Log.d("qiniutest", rinfo.toString());
                        info = rinfo;
                        signal.countDown();
                    }
                }, null);

        try {
            signal.await(6000, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        Assert.assertEquals(info.error, 200, info.statusCode);
//        Assert.assertNull(info.reqId);
    }

    @SmallTest
    public void testPost2() throws Throwable {
        httpManager.asyncPost("https://static-fw.qbox.me/public/v28812/add-on/ga/analytics.js",
                "hello".getBytes(), null, UpToken.parse(TestConfig.token_z0), "hello".getBytes().length,
                null, new CompletionHandler() {
                    @Override
                    public void complete(ResponseInfo rinfo, JSONObject response) {
                        Log.d("qiniutest", rinfo.toString());
                        info = rinfo;
                        signal.countDown();
                    }
                }, null);

        try {
            signal.await(60000, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        Assert.assertEquals(info.error, 200, info.statusCode);
//        Assert.assertNotNull(info.reqId);
    }

//    @SmallTest
//    public void testPost3() throws Throwable {
//        httpManager.asyncPost("https://up.qiniu.com",
//                "hello".getBytes(), null, null, new CompletionHandler() {
//                    @Override
//                    public void complete(ResponseInfo rinfo, JSONObject response) {
//                        Log.d("qiniutest", rinfo.toString());
//                        info = rinfo;
//                        signal.countDown();
//                    }
//                }, null);
//
//        try {
//            signal.await(60000, TimeUnit.SECONDS); // wait for callback
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        // cert is not match
//        Assert.assertEquals(info.error, -1, info.statusCode);
//    }
}
