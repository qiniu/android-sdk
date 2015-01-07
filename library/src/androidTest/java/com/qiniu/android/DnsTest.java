package com.qiniu.android;

import android.test.AndroidTestCase;

import com.qiniu.android.utils.Dns;

import junit.framework.Assert;

/**
 * Created by bailong on 15/1/5.
 */
public class DnsTest extends AndroidTestCase {
    public void testDns() {
        String ip = Dns.getAddressesString("qiniu.com");
        Assert.assertTrue(!ip.equals(""));
        ip = Dns.getAddressesString("nodns.qiniu.com");
        Assert.assertEquals("", ip);
    }
}
