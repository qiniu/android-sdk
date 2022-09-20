package com.qiniu.android.storage.serverConfig;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.TestConfig;
import com.qiniu.android.BaseTest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ServerConfigTest extends BaseTest {

    @Test
    public void testMonitor() {
        ServerConfigMonitor.removeConfigCache();
        ServerConfigMonitor.startMonitor();
        ServerConfigMonitor.setToken(TestConfig.token_na0);

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                return true;
            }
        }, 30);
    }

    @Test
    public void testServerConfigModel() {
        String serverConfigJsonString = "{\"region\":{\"clear_id\":10,\"clear_cache\":true},\"dns\":{\"enabled\":true,\"clear_id\":10,\"clear_cache\":true,\"doh\":{\"enabled\":true,\"ipv4\":{\"override_default\":true,\"urls\":[\"https://223.5.5.5/dns-query\"]},\"ipv6\":{\"override_default\":true,\"urls\":[\"https://FFAE::EEEE/dns-query\"]}},\"udp\":{\"enabled\":true,\"ipv4\":{\"ips\":[\"223.5.5.5\",\"1.1.1.1\"],\"override_default\":true},\"ipv6\":{\"ips\":[\"FFAE::EEEE\"],\"override_default\":true}}},\"ttl\":86400}";
        try {
            JSONObject jsonObject = new JSONObject(serverConfigJsonString);
            ServerConfig serverConfig = new ServerConfig(jsonObject);
            assertTrue("server config ttl was err", serverConfig.isValid());
            assertTrue("server config dns was null", serverConfig.getDnsConfig() != null);
            assertTrue("server config dns enable was null", serverConfig.getDnsConfig().getEnable() != null);
            assertTrue("server config dns clear id was null", serverConfig.getDnsConfig().getClearId() > 0);
            assertTrue("server config dns udp was null", serverConfig.getDnsConfig().getUdpDnsConfig() != null);
            assertTrue("server config dns udp enable was null", serverConfig.getDnsConfig().getUdpDnsConfig().getEnable() != null);
            assertTrue("server config dns udp ipv4 server was null", serverConfig.getDnsConfig().getUdpDnsConfig().getIpv4Server() != null);
            assertTrue("server config dns udp ipv4 server isOverride was null", serverConfig.getDnsConfig().getUdpDnsConfig().getIpv4Server().getIsOverride());
            assertTrue("server config dns udp ipv4 server servers was null", serverConfig.getDnsConfig().getUdpDnsConfig().getIpv4Server().getServers() != null);
            assertTrue("server config dns udp ipv6 server was null", serverConfig.getDnsConfig().getUdpDnsConfig().getIpv6Server() != null);
            assertTrue("server config dns udp ipv6 server isOverride was null", serverConfig.getDnsConfig().getUdpDnsConfig().getIpv6Server().getIsOverride());
            assertTrue("server config dns udp ipv6 server servers was null", serverConfig.getDnsConfig().getUdpDnsConfig().getIpv6Server().getServers() != null);
            assertTrue("server config dns doh was null", serverConfig.getDnsConfig().getDohDnsConfig() != null);
            assertTrue("server config dns doh enable was null", serverConfig.getDnsConfig().getDohDnsConfig().getEnable() != null);
            assertTrue("server config dns doh ipv4 server was null", serverConfig.getDnsConfig().getDohDnsConfig().getIpv4Server() != null);
            assertTrue("server config dns doh ipv4 server isOverride was null", serverConfig.getDnsConfig().getDohDnsConfig().getIpv4Server().getIsOverride());
            assertTrue("server config dns doh ipv4 server servers was null", serverConfig.getDnsConfig().getDohDnsConfig().getIpv4Server().getServers() != null);
            assertTrue("server config dns doh ipv6 server was null", serverConfig.getDnsConfig().getDohDnsConfig().getIpv6Server() != null);
            assertTrue("server config dns doh ipv6 server isOverride was null", serverConfig.getDnsConfig().getDohDnsConfig().getIpv6Server().getIsOverride());
            assertTrue("server config dns doh ipv6 server servers was null", serverConfig.getDnsConfig().getDohDnsConfig().getIpv6Server().getServers() != null);
            assertTrue("server config region was null", serverConfig.getRegionConfig() != null);
            assertTrue("server config region clear id was null", serverConfig.getRegionConfig().getClearId() > 0);
        } catch (JSONException e) {
            fail("server config exception:" + e);
        }

        String serverUserConfigJsonString = "{\"ttl\":86400,\"http3\":{\"enabled\":true},\"network_check\":{\"enabled\":true}}";
        try {
            JSONObject jsonObject = new JSONObject(serverUserConfigJsonString);
            ServerUserConfig serverUserConfig = new ServerUserConfig(jsonObject);
            assertTrue("server user config ttl was error", serverUserConfig.isValid());
            assertTrue("server user config http3 enable was null", serverUserConfig.getHttp3Enable() != null);
            assertTrue("server user config network check enable id was null", serverUserConfig.getNetworkCheckEnable());
        } catch (JSONException e) {
            fail("server config exception:" + e);
        }
    }
}
