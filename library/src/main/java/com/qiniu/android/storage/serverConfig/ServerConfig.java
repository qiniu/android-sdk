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
        private Boolean enable;
        private long clearId;
        private boolean clearCache;

        RegionConfig(JSONObject info) {
            if (info == null) {
                return;
            }

            if (info.opt("enable") != null) {
                this.enable = info.optBoolean("enable");
            }
            this.clearId = info.optLong("clear_id");
            this.clearCache = info.optBoolean("clear_cache", false);
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

    static class UdpDnsConfig {
        private Boolean enable;
        private String[] ipv4Server;
        private String[] ipv6Server;

        UdpDnsConfig(JSONObject info) {
            if (info == null) {
                return;
            }

            if (info.opt("enable") != null) {
                this.enable = info.optBoolean("enable");
            }

            List<String> ipv4Server = new ArrayList<>();
            JSONArray ipv4JsonArray = info.optJSONArray("ipv4");
            if (ipv4JsonArray != null) {
                int length = ipv4JsonArray.length();
                for (int i = 0; i < length; i++) {
                    String server = ipv4JsonArray.optString(i, null);
                    if (server != null) {
                        ipv4Server.add(server);
                    }
                }
            }
            this.ipv4Server = ipv4Server.toArray(new String[0]);

            List<String> ipv6Server = new ArrayList<>();
            JSONArray ipv6JsonArray = info.optJSONArray("ipv6");
            if (ipv6JsonArray != null) {
                int length = ipv6JsonArray.length();
                for (int i = 0; i < length; i++) {
                    String server = ipv6JsonArray.optString(i, null);
                    if (server != null) {
                        ipv6Server.add(server);
                    }
                }
            }
            this.ipv6Server = ipv6Server.toArray(new String[0]);
        }

        Boolean getEnable() {
            return enable;
        }

        String[] getIpv4Server() {
            return ipv4Server;
        }

        String[] getIpv6Server() {
            return ipv6Server;
        }
    }

    static class DohDnsConfig {
        private Boolean enable;
        private String[] ipv4Server;
        private String[] ipv6Server;

        DohDnsConfig(JSONObject info) {
            if (info == null) {
                return;
            }

        }

        Boolean getEnable() {
            return enable;
        }

        String[] getIpv4Server() {
            return ipv4Server;
        }

        String[] getIpv6Server() {
            return ipv6Server;
        }
    }
}
