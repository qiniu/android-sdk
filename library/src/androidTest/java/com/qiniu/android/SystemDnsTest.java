package com.qiniu.android;

import com.qiniu.android.http.dns.IDnsNetworkAddress;
import com.qiniu.android.http.dns.SystemDns;
import com.qiniu.android.utils.Utils;

import java.net.UnknownHostException;
import java.util.List;

public class SystemDnsTest extends BaseTest {

    public void testDnsLookup() {
        SystemDns dns = new SystemDns(5);
        try {
            List<IDnsNetworkAddress> result = dns.lookup("upload.qiniup.com");
            assertTrue("testDnsLookup fail:", result != null && result.size() == 0);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            fail("testDnsLookup fail because:" + e.getMessage());
        }
    }

    public void testDnsTimeout() {
        long start = Utils.currentSecondTimestamp();
        int timeout = 5;
        SystemDns dns = new SystemDns(timeout);
        try {
            List<IDnsNetworkAddress> result = dns.lookup("a.a.a.cn");
            assertTrue("testDnsTimeout fail:", result == null || result.size() == 0);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        long end = Utils.currentSecondTimestamp();
        assertTrue("testDnsTimeout fail because timeout to long", end <= (start + timeout + 1));
    }
}
