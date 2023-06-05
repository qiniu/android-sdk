package com.qiniu.android.storage;


import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.BaseTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;


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
        List<String> dohIpv4Servers = Arrays.asList(configuration.getDohIpv4Servers());
        List<String> udpDnsIpv4Servers = Arrays.asList(configuration.getUdpDnsIpv4Servers());
        List<String> connectCheckURLStrings = Arrays.asList(configuration.getConnectCheckUrls());
        assertTrue("dohIpv4Servers cfg error", dohIpv4Servers.contains("https://8.8.8.8/dns-query"));
        assertTrue("udpDnsIpv4Servers cfg error", udpDnsIpv4Servers.contains("8.8.8.8"));
        assertTrue("connectCheckURLStrings cfg error", connectCheckURLStrings.contains("https://www.qiniu.com"));
    }

}
