package com.qiniu.android.storage.serverConfig;

import com.qiniu.android.utils.Cache;
import com.qiniu.android.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * server config
 *
 * @hidden
 */
public class ServerConfig implements Cache.Object {

    private long timestamp;
    private long ttl = 10;
    private JSONObject info;
    private RegionConfig regionConfig;
    private DnsConfig dnsConfig;
    private ConnectCheckConfig connectCheckConfig;

    /**
     * 构造函数
     *
     * @param info json info
     */
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

    /**
     * 获取 json 数据
     *
     * @return JSONObject
     */
    @Override
    public JSONObject toJson() {
        return info;
    }

    /**
     * 获取 json 数据
     *
     * @return JSONObject
     */
    public JSONObject getInfo() {
        return toJson();
    }

    /**
     * 获取区域配置
     *
     * @return 区域配置
     */
    public RegionConfig getRegionConfig() {
        return regionConfig;
    }

    /**
     * 获取 DNS 配置
     *
     * @return DNS 配置
     */
    public DnsConfig getDnsConfig() {
        return dnsConfig;
    }

    /**
     * 获取网络连接检测配置
     *
     * @return 网络连接检测配置
     */
    public ConnectCheckConfig getConnectCheckConfig() {
        return connectCheckConfig;
    }

    /**
     * 配置信息是否有效
     *
     * @return 配置信息是否有效
     */
    public boolean isValid() {
        return Utils.currentSecondTimestamp() < (this.timestamp + this.ttl);
    }

    /**
     * 区域配置
     *
     * @hidden
     */
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

        /**
         * 获取 clearId
         *
         * @return clearId
         */
        public long getClearId() {
            return clearId;
        }

        /**
         * 获取 clearCache
         *
         * @return clearCache
         */
        public boolean getClearCache() {
            return clearCache;
        }
    }

    /**
     * DNS 配置
     *
     * @hidden
     */
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

        /**
         * DNS 是否生效
         *
         * @return DNS 是否生效
         */
        public Boolean getEnable() {
            return enable;
        }

        /**
         * 获取 clearId
         *
         * @return clearId
         */
        public long getClearId() {
            return clearId;
        }

        /**
         * 获取 clearCache
         *
         * @return clearCache
         */
        public boolean getClearCache() {
            return clearCache;
        }

        /**
         * 获取 udp dns 配置信息
         *
         * @return udp dns 配置信息
         */
        public UdpDnsConfig getUdpDnsConfig() {
            return udpDnsConfig;
        }

        /**
         * 获取 doh 配置信息
         *
         * @return doh 配置信息
         */
        public DohDnsConfig getDohDnsConfig() {
            return dohDnsConfig;
        }
    }

    /**
     * dns server
     *
     * @hidden
     */
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

        /**
         * 是否覆盖配置
         *
         * @return 是否覆盖配置
         */
        public boolean getIsOverride() {
            return isOverride;
        }

        /**
         * 获取 servers
         *
         * @return servers
         */
        public String[] getServers() {
            return servers;
        }
    }

    /**
     * udp dns 配置
     *
     * @hidden
     */
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

        /**
         * 配置是否生效
         *
         * @return 配置是否生效
         */
        public Boolean getEnable() {
            return enable;
        }

        /**
         * 获取 ipv4 server
         *
         * @return ipv4 server
         */
        public DnsServer getIpv4Server() {
            return ipv4Server;
        }

        /**
         * 获取 ipv6 server
         *
         * @return ipv6 server
         */
        public DnsServer getIpv6Server() {
            return ipv6Server;
        }
    }

    /**
     * doh 配置
     *
     * @hidden
     */
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

        /**
         * 配置是否生效
         *
         * @return 配置是否生效
         */
        public Boolean getEnable() {
            return enable;
        }

        /**
         * 获取 ipv4 server
         *
         * @return ipv4 server
         */
        public DnsServer getIpv4Server() {
            return ipv4Server;
        }

        /**
         * 获取 ipv6 server
         *
         * @return ipv6 server
         */
        public DnsServer getIpv6Server() {
            return ipv6Server;
        }
    }

    /**
     * connect check config
     *
     * @hidden
     */
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

        /**
         * 是否覆盖配置
         *
         * @return 是否覆盖配置
         */
        public Boolean getOverride() {
            return isOverride;
        }

        /**
         * 配置是否生效
         *
         * @return 配置是否生效
         */
        public Boolean getEnable() {
            return enable;
        }

        /**
         * 获取超时时间
         *
         * @return 超时时间
         */
        public Integer getTimeoutMs() {
            return timeoutMs;
        }

        /**
         * 获取网络检测的 url 列表
         *
         * @return 网络检测的 url 列表
         */
        public String[] getUrls() {
            return urls;
        }
    }
}
