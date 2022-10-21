package com.qiniu.android.utils;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.BaseTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UtilsTest extends BaseTest {

    @Test
    public void testIPType(){

        String testHost = "host";
        String type = "";

        type = Utils.getIpType("10.10.120.3", testHost);
        assertTrue(type.equals(testHost + "-10-10"));

        type = Utils.getIpType("130.101.120.3", testHost);
        assertTrue(type.equals(testHost + "-130-101"));

        type = Utils.getIpType("2000:0000:0000:0000:0001:2345:6789:abcd", testHost);
        assertTrue(type.equals(testHost + "-ipv6-2000-0000-0000-0000"));

        type = Utils.getIpType("2000:0:0:0:0001:2345:6789:abcd", testHost);
        assertTrue(type.equals(testHost + "-ipv6-2000-0000-0000-0000"));

        type = Utils.getIpType("2000::0001:2345:6789:abcd", testHost);
        assertTrue(type.equals(testHost + "-ipv6-2000-0000-0000-0000"));
    }

    @Test
    public void testIsIPType(){

        String ip = null;
        boolean isIpv6 = false;

        ip = "10.10.120.3";
        isIpv6 = Utils.isIpv6(ip);
        assertFalse(ip, isIpv6);

        ip = "130.101.120.3";
        isIpv6 = Utils.isIpv6(ip);
        assertFalse(ip, isIpv6);

        ip = "2000:0000:0000:0000:0001:2345:6789:abcd";
        isIpv6 = Utils.isIpv6(ip);
        assertTrue(ip, isIpv6);

        ip = "2000:0:0:0:0001:2345:6789:abcd";
        isIpv6 = Utils.isIpv6(ip);
        assertTrue(ip, isIpv6);

        ip = "2000::0001:2345:6789:abcd";
        isIpv6 = Utils.isIpv6(ip);
        assertTrue(ip, isIpv6);

        ip = "0::0";
        isIpv6 = Utils.isIpv6(ip);
        assertTrue(ip, isIpv6);

        ip = "ffff::ffff:2345:6789:abcd";
        isIpv6 = Utils.isIpv6(ip);
        assertTrue(ip, isIpv6);

        ip = "ff1::ffff:2345:6789:abcd";
        isIpv6 = Utils.isIpv6(ip);
        assertTrue(ip, isIpv6);

        ip = "ffff1::ffff:2345:6789:abcd";
        isIpv6 = Utils.isIpv6(ip);
        assertFalse(ip, isIpv6);
    }
}
