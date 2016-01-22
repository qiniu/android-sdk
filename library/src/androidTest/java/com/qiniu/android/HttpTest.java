package com.qiniu.android;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.qiniu.android.http.CompletionHandler;
import com.qiniu.android.http.HttpManager;
import com.qiniu.android.http.ResponseInfo;

import junit.framework.Assert;

import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.message.BasicHeader;

/**
 * Created by bailong on 14/10/12.
 */
public class HttpTest extends InstrumentationTestCase {
    final CountDownLatch signal = new CountDownLatch(1);
    private HttpManager httpManager;
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
        httpManager = new HttpManager();
    }

    @SmallTest
    public void testPost1() throws Throwable {
        httpManager.postData(newURI("http://www.baidu.com"), "hello".getBytes(), null, null, new CompletionHandler() {
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
        Assert.assertNull(info.reqId);
    }

    @SmallTest
    public void testPost2() throws Throwable {

        httpManager.postData(newURI("http://up.qiniu.com"), "hello".getBytes(), null, null, new CompletionHandler() {
            @Override
            public void complete(ResponseInfo rinfo, JSONObject response) {
                Log.d("qiniutest", rinfo.toString());
                info = rinfo;
                signal.countDown();
            }
        }, null);

        try {
            signal.await(60, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertNotNull(info.reqId);
    }

    @SmallTest
    public void testPost3() throws Throwable {
        runTestOnUiThread(new Runnable() { // THIS IS THE KEY TO SUCCESS
            public void run() {
                httpManager.postData(newURI("http://httpbin.org/status/500"), "hello".getBytes(),
                        null, null, new CompletionHandler() {
                            @Override
                            public void complete(ResponseInfo rinfo, JSONObject response) {
                                Log.d("qiniutest", rinfo.toString());
                                info = rinfo;
                                signal.countDown();
                            }
                        }, null);
            }
        });

        try {
            signal.await(60, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(500, info.statusCode);
        Assert.assertNotNull(info.error);
    }

    @SmallTest
    public void testPost4() throws Throwable {
        runTestOnUiThread(new Runnable() { // THIS IS THE KEY TO SUCCESS
            public void run() {
                httpManager.postData(newURI("http://httpbin.org/status/418"), "hello".getBytes(),
                        null, null, new CompletionHandler() {
                            @Override
                            public void complete(ResponseInfo rinfo, JSONObject response) {
                                Log.d("qiniutest", rinfo.toString());
                                info = rinfo;
                                signal.countDown();
                            }
                        }, null);
            }
        });

        try {
            signal.await(60, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(418, info.statusCode);
        Assert.assertNotNull(info.error);
    }

    @SmallTest
    public void testPostNoDomain() throws Throwable {

        httpManager.postData(newURI("http://no-domain.qiniu.com"), "hello".getBytes(),
                null, null, new CompletionHandler() {
                    @Override
                    public void complete(ResponseInfo rinfo, JSONObject response) {
                        Log.d("qiniutest", rinfo.toString());
                        info = rinfo;
                        signal.countDown();
                    }
                }, null);

        try {
            signal.await(60, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertNull(info.reqId);
        Assert.assertEquals(ResponseInfo.UnknownHost, info.statusCode);
    }

    @SmallTest
    public void testPostNoPort() throws Throwable {

        httpManager.postData(newURI("http://up.qiniu.com:12345"), "hello".getBytes(),
                null, null, new CompletionHandler() {
                    @Override
                    public void complete(ResponseInfo rinfo, JSONObject response) {
                        Log.d("qiniutest", rinfo.toString());
                        info = rinfo;
                        signal.countDown();
                    }
                }, null);

        try {
            signal.await(60, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertNull(info.reqId);
        Assert.assertEquals(ResponseInfo.CannotConnectToHost, info.statusCode);
    }

    @SmallTest
    public void testPostIP() throws Throwable {
        Header[] x = {new BasicHeader("Host", "www.qiniu.com")};
        httpManager.postData(newURI("http://183.136.139.12/"), "hello".getBytes(),
                x, null, new CompletionHandler() {
                    @Override
                    public void complete(ResponseInfo rinfo, JSONObject response) {
                        Log.d("qiniutest", rinfo.toString());
                        info = rinfo;
                        signal.countDown();
                    }
                }, null);

        try {
            signal.await(60, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertNotNull(info.reqId);
        Assert.assertEquals(200, info.statusCode);
    }
}
