package com.qiniu.android.storage.serverConfig;

import com.qiniu.android.common.AutoZone;
import com.qiniu.android.http.dns.DnsPrefetcher;
import com.qiniu.android.storage.GlobalConfiguration;
import com.qiniu.android.transaction.TransactionManager;

import java.util.List;

public class ServerConfigMonitor {
    private static final String TransactionKey = "ServerConfig";

    private boolean enable = true;
    private ServerConfigCache cache = new ServerConfigCache();
    private static ServerConfigMonitor configMonitor = new ServerConfigMonitor();

    public static void setEnable(boolean enable) {
        configMonitor.enable = enable;
    }

    public static void setToken(String token) {
        ServerConfigSynchronizer.setToken(token);
    }

    public static void setServerHosts(String[] hosts) {
        ServerConfigSynchronizer.setHosts(hosts);
    }

    // 开始监控
    public synchronized static void startMonitor() {
        if (!configMonitor.enable) {
            return;
        }

        TransactionManager transactionManager = TransactionManager.getInstance();
        boolean isExist = transactionManager.existTransactionsForName(TransactionKey);
        if (isExist) {
            return;
        }

        TransactionManager.Transaction transaction = new TransactionManager.Transaction(TransactionKey, 0, 10, new Runnable() {
            @Override
            public void run() {
                configMonitor.monitor();
            }
        });
        transactionManager.addTransaction(transaction);
    }

    // 停止监控
    public synchronized static void endMonitor() {
        TransactionManager transactionManager = TransactionManager.getInstance();
        List<TransactionManager.Transaction> transactions = transactionManager.transactionsForName(TransactionKey);
        if (transactions != null) {
            for (TransactionManager.Transaction transaction : transactions) {
                transactionManager.removeTransaction(transaction);
            }
        }
    }

    private void monitor() {
        if (!enable) {
            return;
        }

        if (cache.getConfig() == null) {
            ServerConfig config = cache.getConfigFromDisk();
            handleServerConfig(config);
            cache.setConfig(config);
        }

        ServerConfig serverConfig = cache.getConfig();
        if (serverConfig == null || !serverConfig.isValid()) {
            ServerConfigSynchronizer.getServerConfigFromServer(new ServerConfigSynchronizer.ServerConfigHandler() {
                @Override
                public void handle(ServerConfig config) {
                    if (config == null) {
                        return;
                    }

                    handleServerConfig(config);
                    cache.setConfig(config);
                    cache.saveConfigToDisk(config);
                }
            });
        }

        if (cache.getUserConfig() == null) {
            ServerUserConfig config = cache.getUserConfigFromDisk();
            handleServerUserConfig(config);
            cache.setUserConfig(config);
        }

        ServerUserConfig serverUserConfig = cache.getUserConfig();
        if (serverUserConfig == null || !serverUserConfig.isValid()) {
            ServerConfigSynchronizer.getServerUserConfigFromServer(new ServerConfigSynchronizer.ServerUserConfigHandler() {
                @Override
                public void handle(ServerUserConfig config) {
                    if (config == null) {
                        return;
                    }

                    handleServerUserConfig(config);
                    cache.setUserConfig(config);
                    cache.setUserConfigToDisk(config);
                }
            });
        }
    }

    private void handleServerConfig(ServerConfig config) {
        if (config == null) {
            return;
        }

        final ServerConfig serverConfig = cache.getConfig();
        // 清理 region 缓存
        ServerConfig.RegionConfig regionConfig = config.getRegionConfig();
        ServerConfig.RegionConfig oldRegionConfig = serverConfig != null ? serverConfig.getRegionConfig() : null;
        if (regionConfig != null) {
            if (oldRegionConfig != null && regionConfig.getClearId() > oldRegionConfig.getClearId() && regionConfig.getClearCache()) {
                AutoZone.clearCache();
            }
        }

        // dns 配置
        ServerConfig.DnsConfig dnsConfig = config.getDnsConfig();
        if (dnsConfig != null) {
            if (dnsConfig.getEnable() != null) {
                GlobalConfiguration.getInstance().isDnsOpen = dnsConfig.getEnable();
            }

            // 清理 dns 缓存
            ServerConfig.DnsConfig oldDnsConfig = serverConfig != null ? serverConfig.getDnsConfig() : null;
            if (oldDnsConfig != null && dnsConfig.getClearId() > oldDnsConfig.getClearId() && dnsConfig.getClearCache()) {
                try {
                    DnsPrefetcher.getInstance().clearDnsCache();
                } catch (Exception ignored) {
                }
            }

            // udp 配置
            ServerConfig.UdpDnsConfig udpDnsConfig = dnsConfig.getUdpDnsConfig();
            if (udpDnsConfig != null) {
                if (udpDnsConfig.getEnable() != null) {
                    GlobalConfiguration.getInstance().udpDnsEnable = udpDnsConfig.getEnable();
                }

                ServerConfig.DnsServer ipv4Servers = udpDnsConfig.getIpv4Server();
                if (ipv4Servers != null && ipv4Servers.getIsOverride()) {
                    GlobalConfiguration.DefaultUdpDnsIpv4Servers = ipv4Servers.getServers();
                }

                ServerConfig.DnsServer ipv6Servers = udpDnsConfig.getIpv6Server();
                if (ipv6Servers != null && ipv6Servers.getIsOverride()) {
                    GlobalConfiguration.DefaultUdpDnsIpv6Servers = ipv6Servers.getServers();
                }
            }

            // doh 配置
            ServerConfig.DohDnsConfig dohConfig = dnsConfig.getDohDnsConfig();
            if (dohConfig != null) {
                if (dohConfig.getEnable() != null) {
                    GlobalConfiguration.getInstance().dohEnable = dohConfig.getEnable();
                }

                ServerConfig.DnsServer ipv4Servers = dohConfig.getIpv4Server();
                if (ipv4Servers != null && ipv4Servers.getIsOverride()) {
                    GlobalConfiguration.DefaultUdpDnsIpv4Servers = ipv4Servers.getServers();
                }

                ServerConfig.DnsServer ipv6Servers = dohConfig.getIpv6Server();
                if (ipv6Servers != null && ipv6Servers.getIsOverride()) {
                    GlobalConfiguration.DefaultDohIpv4Servers = ipv6Servers.getServers();
                }
            }
        }
    }

    private void handleServerUserConfig(ServerUserConfig config) {
        if (config == null) {
            return;
        }

        if (config.getNetworkCheckEnable() != null) {
            GlobalConfiguration.getInstance().connectCheckEnable = config.getNetworkCheckEnable();
        }
    }
}
