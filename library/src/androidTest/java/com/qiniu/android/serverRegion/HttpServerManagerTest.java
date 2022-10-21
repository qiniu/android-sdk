package com.qiniu.android.serverRegion;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.BaseTest;
import com.qiniu.android.http.serverRegion.HttpServerManager;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class HttpServerManagerTest extends BaseTest {

    @Test
    public void testServer() {
        String host = "up.qiniu.com";
        String ip = "192.168.1.1";
        int duration = 10;
        HttpServerManager manager = HttpServerManager.getInstance();
        assertFalse("null host and ip add success", manager.addHttp3Server(null, null, duration));
        assertFalse("null host add success", manager.addHttp3Server(null, ip, duration));
        assertFalse("null ip add success", manager.addHttp3Server(host, null, duration));
        assertFalse("empty host and ip add success", manager.addHttp3Server("", "", duration));
        assertFalse("empty host add success", manager.addHttp3Server("", ip, duration));
        assertFalse("empty ip add success", manager.addHttp3Server(host, "", duration));
        assertFalse("liveDuration < 0 add success", manager.addHttp3Server(host, ip, -1));

        manager.addHttp3Server(host, ip, duration);
        assertTrue("host ip should support", manager.isServerSupportHttp3(host, ip));

        assertFalse("null host and ip should not support", manager.isServerSupportHttp3(null, null));
        assertFalse("null host should not support", manager.isServerSupportHttp3(null, ip));
        assertFalse("null ip should not support", manager.isServerSupportHttp3(host, null));

        assertFalse("empty host and ip should not support", manager.isServerSupportHttp3("", ""));
        assertFalse("empty host should not support", manager.isServerSupportHttp3("", ip));
        assertFalse("empty ip should not support", manager.isServerSupportHttp3(host, "ip"));

        assertFalse("no exist host and ip should not support", manager.isServerSupportHttp3("host", "ip"));
        assertFalse("no exist hos should not support", manager.isServerSupportHttp3("host", ip));
        assertFalse("no exist ip should not support", manager.isServerSupportHttp3(host, "ip"));
    }
}
