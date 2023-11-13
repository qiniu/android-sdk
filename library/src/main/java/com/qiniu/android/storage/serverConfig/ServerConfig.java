package com.qiniu.android.storage.serverConfig;

import com.qiniu.android.utils.Cache;
import com.qiniu.android.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ServerConfig implements Cache.Object {

    private long timestamp;
    private long ttl = 10;
    private JSONObject info;
    private RegionConfig regionConfig;
    private DnsConfig dnsConfig;
    private ConnectCheckConfig connectCheckConfig;

    public ServerConfig(JSONObject info) {
        if (info == null) {
            return;
        }

        this.info = info;

        this.ttl = info.optLong("ttl", 5 * 60);

        if (info.opt("timestamp") != null) {
            this.timestamp = info.optLong("timestamp");
        }
        if (this.timestamp == 0) {
            this.timestamp = Utils.currentSecondTimestamp();
            try {
                info.putOpt("timestamp", this.timestamp);
            } catch (JSONException ignored) {
            }
        }

        this.dnsConfig = new DnsConfig(info.optJSONObject("dns"));
        this.regionConfig = new RegionConfig(info.optJSONObject("region"));
        this.connectCheckConfig = new ConnectCheckConfig(info.optJSONObject("connection_check"));

        if (this.ttl < 10) {
            this.ttl = 10;
        }
    }

    @Override
    public JSONObject toJson() {
        return info;
    }

    public JSONObject getInfo() {
        return toJson();
    }

    public RegionConfig getRegionConfig() {
        return regionConfig;
    }

    public DnsConfig getDnsConfig() {
        return dnsConfig;
    }

    public ConnectCheckConfig getConnectCheckConfig() {
        return connectCheckConfig;
    }

    public boolean isValid() {
        return Utils.currentSecondTimestamp() < (this.timestamp + this.ttl);
    }

    public static class RegionConfig {
        private long clearId;
        private boolean clearCache;

        RegionConfig(JSONObject info) {
            if (info == null) {
                return;
            }

            this.clearId = info.optLong("clear_id");
            this.clearCache = info.optBoolean("clear_cache", false);
        }

        public long getClearId() {
            return clearId;
        }

        public boolean getClearCache() {
            return clearCache;
        }
    }

    public static class DnsConfig {
        private Boolean enable;
        private long clearId;
        private boolean clearCache = false;
        private UdpDnsConfig udpDnsConfig;
        private DohDnsConfig dohDnsConfig;

        DnsConfig(JSONObject info) {
            if (info == null) {
                return;
            }

            if (info.opt("enabled") != null) {
                this.enable = info.optBoolean("enabled");
            }
            this.clearId = info.optLong("clear_id");
            this.clearCache = info.optBoolean("clear_cache", false);
            this.udpDnsConfig = new UdpDnsConfig(info.optJSONObject("udp"));
            this.dohDnsConfig = new DohDnsConfig(info.optJSONObject("doh"));
        }

        public Boolean getEnable() {
            return enable;
        }

        public long getClearId() {
            return clearId;
        }

        public boolean getClearCache() {
            return clearCache;
        }

        public UdpDnsConfig getUdpDnsConfig() {
            return udpDnsConfig;
        }

        public DohDnsConfig getDohDnsConfig() {
            return dohDnsConfig;
        }
    }

    public static class DnsServer {
        private boolean isOverride;
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

        public boolean getIsOverride() {
            return isOverride;
        }

        public String[] getServers() {
            return servers;
        }
    }

    public static class UdpDnsConfig {
        private Boolean enable;
        private DnsServer ipv4Server;
        private DnsServer ipv6Server;

        UdpDnsConfig(JSONObject info) {
            if (info == null) {
                return;
            }

            if (info.opt("enabled") != null) {
                this.enable = info.optBoolean("enabled");
            }

            this.ipv4Server = new DnsServer(info.optJSONObject("ipv4"));
            this.ipv6Server = new DnsServer(info.optJSONObject("ipv6"));
        }

        public Boolean getEnable() {
            return enable;
        }

        public DnsServer getIpv4Server() {
            return ipv4Server;
        }

        public DnsServer getIpv6Server() {
            return ipv6Server;
        }
    }

    public static class DohDnsConfig {
        private Boolean enable;
        private DnsServer ipv4Server;
        private DnsServer ipv6Server;

        DohDnsConfig(JSONObject info) {
            if (info == null) {
                return;
            }

            if (info.opt("enabled") != null) {
                this.enable = info.optBoolean("enabled");
            }

            this.ipv4Server = new DnsServer(info.optJSONObject("ipv4"));
            this.ipv6Server = new DnsServer(info.optJSONObject("ipv6"));
        }

        public Boolean getEnable() {
            return enable;
        }

        public DnsServer getIpv4Server() {
            return ipv4Server;
        }

        public DnsServer getIpv6Server() {
            return ipv6Server;
        }
    }

    public static class ConnectCheckConfig {
        private Boolean isOverride;
        private Boolean enable;
        private Integer timeoutMs;
        private String[] urls;

        ConnectCheckConfig(JSONObject info) {
            if (info == null) {
                return;
            }

            if (info.opt("override_default") != null) {
                this.isOverride = info.optBoolean("override_default");
            }

            if (info.opt("enabled") != null) {
                this.enable = info.optBoolean("enabled");
            }

            if (info.opt("timeout_ms") != null) {
                this.timeoutMs = info.optInt("timeout_ms");
            }

            if (info.opt("urls") != null) {
                try {
                    JSONArray jsonArray = info.getJSONArray("urls");
                    String[] urls = new String[jsonArray.length()];
                    for (int i = 0; i < jsonArray.length(); i++) {
                        urls[i] = jsonArray.getString(i);
                    }
                    this.urls = urls;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public Boolean getOverride() {
            return isOverride;
        }

        public Boolean getEnable() {
            return enable;
        }

        public Integer getTimeoutMs() {
            return timeoutMs;
        }

        public String[] getUrls() {
            return urls;
        }
    }
}
