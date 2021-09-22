package com.qiniu.android.storage.serverConfig;

import com.qiniu.android.utils.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class ServerConfig {

    private long timestamp;
    private long ttl = 10;
    private JSONObject info;
    private RegionConfig regionConfig;
    private DnsConfig dnsConfig;

    ServerConfig(JSONObject info) {
        this.timestamp = Utils.currentSecondTimestamp();

        if (info == null) {
            return;
        }
        this.info = info;

        this.ttl = info.optLong("ttl", 5 * 60);
        this.dnsConfig = new DnsConfig(info.optJSONObject("dns"));
        this.regionConfig = new RegionConfig(info.optJSONObject("region"));

        if (this.ttl < 10) {
            this.ttl = 10;
        }
    }

    long getTtl() {
        return ttl;
    }

    JSONObject getInfo() {
        return info;
    }

    RegionConfig getRegionConfig() {
        return regionConfig;
    }

    DnsConfig getDnsConfig() {
        return dnsConfig;
    }

    boolean isValid() {
        return Utils.currentSecondTimestamp() < (this.timestamp + this.ttl);
    }

    static class RegionConfig {
        private long clearId;
        private boolean clearCache;

        RegionConfig(JSONObject info) {
            if (info == null) {
                return;
            }

            this.clearId = info.optLong("clear_id");
            this.clearCache = info.optBoolean("clear_cache", false);
        }

        long getClearId() {
            return clearId;
        }

        boolean getClearCache() {
            return clearCache;
        }
    }

    static class DnsConfig {
        private Boolean enable;
        private long clearId;
        private boolean clearCache = false;
        private UdpDnsConfig udpDnsConfig;
        private DohDnsConfig dohDnsConfig;

        DnsConfig(JSONObject info) {
            if (info == null) {
                return;
            }

            if (info.opt("enable") != null) {
                this.enable = info.optBoolean("enable");
            }
            this.clearId = info.optLong("clear_id");
            this.clearCache = info.optBoolean("clear_cache", false);
            this.udpDnsConfig = new UdpDnsConfig(info.optJSONObject("dns"));
            this.dohDnsConfig = new DohDnsConfig(info.optJSONObject("doh"));
        }

        Boolean getEnable() {
            return enable;
        }

        long getClearId() {
            return clearId;
        }

        boolean getClearCache() {
            return clearCache;
        }

        public UdpDnsConfig getUdpDnsConfig() {
            return udpDnsConfig;
        }

        public DohDnsConfig getDohDnsConfig() {
            return dohDnsConfig;
        }
    }

    static class DnsServer {
        private Boolean isOverride;
        private String[] servers;

        DnsServer(JSONObject info) {
            if (info == null) {
                return;
            }

            this.isOverride = info.optBoolean("override_default");

            List<String> servers = new ArrayList<>();
            JSONArray serverJsonArray = info.optJSONArray("ips");
            if (serverJsonArray == null) {
                serverJsonArray = info.optJSONArray("urls");
            }
            if (serverJsonArray != null) {
                int length = serverJsonArray.length();
                for (int i = 0; i < length; i++) {
                    String server = serverJsonArray.optString(i, null);
                    if (server != null) {
                        servers.add(server);
                    }
                }
            }
            this.servers = servers.toArray(new String[0]);
        }

        Boolean getIsOverride() {
            return isOverride;
        }

        String[] getServers() {
            return servers;
        }
    }

    static class UdpDnsConfig {
        private Boolean enable;
        private DnsServer ipv4Server;
        private DnsServer ipv6Server;

        UdpDnsConfig(JSONObject info) {
            if (info == null) {
                return;
            }

            if (info.opt("enable") != null) {
                this.enable = info.optBoolean("enable");
            }

            this.ipv4Server = new DnsServer(info.optJSONObject("ipv4"));
            this.ipv6Server = new DnsServer(info.optJSONObject("ipv6"));
        }

        Boolean getEnable() {
            return enable;
        }

        DnsServer getIpv4Server() {
            return ipv4Server;
        }

        DnsServer getIpv6Server() {
            return ipv6Server;
        }
    }

    static class DohDnsConfig {
        private Boolean enable;
        private DnsServer ipv4Server;
        private DnsServer ipv6Server;

        DohDnsConfig(JSONObject info) {
            if (info == null) {
                return;
            }

            if (info.opt("enable") != null) {
                this.enable = info.optBoolean("enable");
            }

            this.ipv4Server = new DnsServer(info.optJSONObject("ipv4"));
            this.ipv6Server = new DnsServer(info.optJSONObject("ipv6"));
        }

        Boolean getEnable() {
            return enable;
        }

        DnsServer getIpv4Server() {
            return ipv4Server;
        }

        DnsServer getIpv6Server() {
            return ipv6Server;
        }
    }
}
