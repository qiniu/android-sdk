package com.qiniu.android;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.qiniu.android.http.Client;
import com.qiniu.android.http.CompletionHandler;
import com.qiniu.android.http.ProxyConfiguration;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.utils.StringMap;

import junit.framework.Assert;

import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Created by bailong on 14/10/12.
 */
public class HttpTest extends InstrumentationTestCase {
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
        httpManager.asyncPost("http://www.baidu.com",
                "hello".getBytes(), null, UpToken.parse(TestConfig.token_z0), "hello".getBytes().length,
                null, new CompletionHandler() {
                    @Override
                    public void complete(ResponseInfo rinfo, JSONObject response) {
                        Assert.assertNotNull(rinfo);
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

        httpManager.asyncPost("http://up.qiniu.com", "hello".getBytes(), null,
                UpToken.parse(TestConfig.token_z0), "hello".getBytes().length,
                null, new CompletionHandler() {
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
                httpManager.asyncPost("http://httpbin.org/status/500", "hello".getBytes(),
                        null, UpToken.parse(TestConfig.token_z0), "hello".getBytes().length,
                        null, new CompletionHandler() {
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
                httpManager.asyncPost("http://httpbin.org/status/418",
                        "hello".getBytes(),
                        null, UpToken.parse(TestConfig.token_z0), "hello".getBytes().length,
                        null, new CompletionHandler() {
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

        httpManager.asyncPost("http://no-domain.qiniu.com", "hello".getBytes(),
                null, UpToken.parse(TestConfig.token_z0), "hello".getBytes().length,
                null, new CompletionHandler() {
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
//        Assert.assertEquals(null, info.reqId);
//        Assert.assertEquals(ResponseInfo.UnknownHost, info.statusCode);
    }

//    @SmallTest
//    public void testPostNoPort() throws Throwable {
//
//        httpManager.asyncPost("http://up.qiniu.com:12345", "hello".getBytes(),
//                null, null, new CompletionHandler() {
//                    @Override
//                    public void complete(ResponseInfo rinfo, JSONObject response) {
//                        Log.d("qiniutest", rinfo.toString());
//                        info = rinfo;
//                        signal.countDown();
//                    }
//                }, null);
//
//        try {
//            signal.await(60, TimeUnit.SECONDS); // wait for callback
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        Assert.assertEquals("", info.reqId);
//        Assert.assertTrue(ResponseInfo.CannotConnectToHost == info.statusCode ||
//                ResponseInfo.TimedOut == info.statusCode);
//    }

    @SmallTest
    public void testPostIP() throws Throwable {
        StringMap x = new StringMap().put("Host", "up.qiniu.com");
        httpManager.asyncPost("http://183.131.7.18", "hello".getBytes(),
                x, UpToken.parse(TestConfig.token_z0), "hello".getBytes().length,
                null, new CompletionHandler() {
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
        Assert.assertTrue(!"".equals(info.reqId));
        Assert.assertEquals(400, info.statusCode);
    }

    @SmallTest
    public void testProxy() throws Throwable {
        StringMap x = new StringMap();
        ProxyConfiguration p = new ProxyConfiguration("115.231.183.168", 80);
        Client c = new Client(p, 10, 30, null, null);
        c.asyncPost("http://upproxy1.qiniu.com", "hello".getBytes(),
                x, UpToken.parse(TestConfig.token_z0), "hello".getBytes().length,
                null, new CompletionHandler() {
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
        Assert.assertTrue(!"".equals(info.reqId));
        Assert.assertEquals(400, info.statusCode);
    }

    @SmallTest
    public void testHeader() {
        // com.qiniu.android.http.UserAgent#getUa
        // return new String((ua + "; " + _part + ")").getBytes(Charset.forName("ISO-8859-1")));

        String name = new String(("电话☎️的の").getBytes(Charset.forName("ISO-8859-1")));
        String value = new String(("sdf✈️he覆盖😁🆚9837-=/ df").getBytes(Charset.forName("ISO-8859-1")));
        checkNameAndValue(name, value);
    }

    // copy from okhttp3.Headers
    private void checkNameAndValue(String name, String value) {
        if (name == null) throw new IllegalArgumentException("name == null");
        if (name.isEmpty()) throw new IllegalArgumentException("name is empty");
        for (int i = 0, length = name.length(); i < length; i++) {
            char c = name.charAt(i);
            if (c <= '\u001f' || c >= '\u007f') {
                throw new IllegalArgumentException(String.format(
                        "Unexpected char %#04x at %d in header name: %s", (int) c, i, name));
            }
        }
        if (value == null) throw new IllegalArgumentException("value == null");
        for (int i = 0, length = value.length(); i < length; i++) {
            char c = value.charAt(i);
            if (c <= '\u001f' || c >= '\u007f') {
                throw new IllegalArgumentException(String.format(
                        "Unexpected char %#04x at %d in %s value: %s", (int) c, i, name, value));
            }
        }
    }
}
