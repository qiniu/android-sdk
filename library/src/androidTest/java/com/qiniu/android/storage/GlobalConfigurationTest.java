package com.qiniu.android.storage;


import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.BaseTest;

import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Created by yangsen on 2020/6/3
 */
@RunWith(AndroidJUnit4.class)
public class GlobalConfigurationTest extends BaseTest {

    @Test
    public void testUpload() {
        GlobalConfiguration configuration = GlobalConfiguration.getInstance();
        configuration.dohIpv4Servers = null;
        configuration.udpDnsIpv4Servers = null;
        configuration.connectCheckURLStrings = null;
        assertTrue("dohIpv4Servers cfg error", configuration.getDohIpv4Servers()[0].equals("https://8.8.8.8/dns-query"));
        assertTrue("udpDnsIpv4Servers cfg error", configuration.getUdpDnsIpv4Servers()[1].equals("8.8.8.8"));
        assertTrue("connectCheckURLStrings cfg error", configuration.getConnectCheckUrls()[0].equals("https://www.qiniu.com"));
    }

}
