package com.qiniu.android.http;


import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.TestConfig;
import com.qiniu.android.bigdata.client.Client;
import com.qiniu.android.bigdata.client.CompletionHandler;
import com.qiniu.android.BaseTest;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.utils.AsyncRun;
import com.qiniu.android.utils.LogUtil;
import com.qiniu.android.utils.StringMap;

import junit.framework.Assert;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;


/**
 * Created by bailong on 14/10/12.
 */
@RunWith(AndroidJUnit4.class)
public class HttpTest extends BaseTest {
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
    @Before
    public void setUp() throws Exception {
        httpManager = new Client(null, 90, 90, null, null);
    }

    @Test
    public void testPost1() throws Throwable {

        httpManager.asyncPost("https://up-na0.qiniup.com",
                "hello".getBytes(), null, UpToken.parse(TestConfig.commonToken), "hello".getBytes().length,
                null, new CompletionHandler() {
                    @Override
                    public void complete(ResponseInfo rinfo, JSONObject response) {
                        Assert.assertNotNull(rinfo);
                        LogUtil.d(rinfo.toString());
                        info = rinfo;
                    }
                }, null);

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (info == null) {
                    return true;
                } else {
                    return false;
                }
            }
        }, 60);

        assertEquals(info.error, 400, info.statusCode);
        assertTrue("reqid is empty", info.reqId.length() > 0);
    }

    @Test
    public void testPost2() throws Throwable {

        httpManager.asyncPost("https://up.qiniup.com", "hello".getBytes(), null,
                UpToken.parse(TestConfig.commonToken), "hello".getBytes().length,
                null, new CompletionHandler() {
                    @Override
                    public void complete(ResponseInfo rinfo, JSONObject response) {
                        LogUtil.d(rinfo.toString());
                        info = rinfo;
                    }
                }, null);

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (info == null) {
                    return true;
                } else {
                    return false;
                }
            }
        }, 60);

        Assert.assertNotNull(info.reqId);
    }

    @Test
    public void testPost3() throws Throwable {
        AsyncRun.runInMain(new Runnable() { // THIS IS THE KEY TO SUCCESS
            public void run() {

                httpManager.asyncPost("https://httpbin.org/status/500", "hello".getBytes(),
                        null, UpToken.parse(TestConfig.commonToken), "hello".getBytes().length,
                        null, new CompletionHandler() {
                            @Override
                            public void complete(ResponseInfo rinfo, JSONObject response) {
                                LogUtil.d(rinfo.toString());
                                info = rinfo;
                            }
                        }, null);
            }
        });

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (info == null) {
                    return true;
                } else {
                    return false;
                }
            }
        }, 60);

        Assert.assertEquals(500, info.statusCode);
        Assert.assertNotNull(info.error);
    }

    @Test
    public void testPost4() throws Throwable {
        AsyncRun.runInMain(new Runnable() { // THIS IS THE KEY TO SUCCESS
            public void run() {
                httpManager.asyncPost("https://httpbin.org/status/418",
                        "hello".getBytes(),
                        null, UpToken.parse(TestConfig.commonToken), "hello".getBytes().length,
                        null, new CompletionHandler() {
                            @Override
                            public void complete(ResponseInfo rinfo, JSONObject response) {
                                LogUtil.d(rinfo.toString());
                                info = rinfo;
                            }
                        }, null);
            }
        });

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (info == null) {
                    return true;
                } else {
                    return false;
                }
            }
        }, 60);

        Assert.assertEquals(418, info.statusCode);
        Assert.assertNotNull(info.error);
    }

    private void testPostNoDomain() throws Throwable {


        httpManager.asyncPost("https://no-domain.qiniu.com", "hello".getBytes(),
                null, UpToken.parse(TestConfig.commonToken), "hello".getBytes().length,
                null, new CompletionHandler() {
                    @Override
                    public void complete(ResponseInfo rinfo, JSONObject response) {
                        LogUtil.d(rinfo.toString());
                        info = rinfo;
                    }
                }, null);

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (info == null) {
                    return true;
                } else {
                    return false;
                }
            }
        }, 60);
        Assert.assertEquals("", info.reqId);
        Assert.assertEquals(ResponseInfo.UnknownHost, info.statusCode);
    }

//    @SmallTest
//    public void testPostNoPort() throws Throwable {
//
//        httpManager.asyncPost("http://up.qiniu.com:12345", "hello".getBytes(),
//                null, null, new CompletionHandler() {
//                    @Override
//                    public void complete(ResponseInfo rinfo, JSONObject response) {
//                        LogUtil.d(rinfo.toString());
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


    private void testPostIP() throws Throwable {
        info = null;
        StringMap x = new StringMap().put("Host", "up.qiniu.com");

        httpManager.asyncPost("http://124.160.115.112", "hello".getBytes(),
                x, UpToken.parse(TestConfig.commonToken), "hello".getBytes().length,
                null, new CompletionHandler() {
                    @Override
                    public void complete(ResponseInfo rinfo, JSONObject response) {
                        LogUtil.d(rinfo.toString());
                        info = rinfo;
                    }
                }, null);

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (info == null) {
                    return true;
                } else {
                    return false;
                }
            }
        }, 60);

        Assert.assertTrue(!"".equals(info.reqId));
        Assert.assertEquals(400, info.statusCode);
    }

    private void testProxy() throws Throwable {
        StringMap x = new StringMap();
        ProxyConfiguration p = new ProxyConfiguration("115.238.101.32", 80);
        Client c = new Client(p, 10, 30, null, null);

        c.asyncPost("http://upproxy1.qiniu.com", "hello".getBytes(),
                x, UpToken.parse(TestConfig.commonToken), "hello".getBytes().length,
                null, new CompletionHandler() {
                    @Override
                    public void complete(ResponseInfo rinfo, JSONObject response) {
                        LogUtil.d(rinfo.toString());
                        info = rinfo;
                    }
                }, null);

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (info == null) {
                    return true;
                } else {
                    return false;
                }
            }
        }, 60);

        assertTrue(info.reqId.length() > 0);
        assertEquals(400, info.statusCode);
    }

    @Test
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
