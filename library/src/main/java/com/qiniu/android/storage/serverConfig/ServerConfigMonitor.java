package com.qiniu.android.storage.serverConfig;

import com.qiniu.android.common.AutoZone;
import com.qiniu.android.http.dns.DnsPrefetcher;
import com.qiniu.android.storage.GlobalConfiguration;
import com.qiniu.android.transaction.TransactionManager;

import java.util.List;

public class ServerConfigMonitor {
    private static final String TransactionKey = "ServerConfig";

    private String token;
    private ServerConfigCache cache = new ServerConfigCache();

    private int monitorTaskCount = 0;
    private static ServerConfigMonitor configMonitor = new ServerConfigMonitor();

    public static ServerConfigMonitor getInstance() {
        return configMonitor;
    }


    // 开始监控
    public synchronized void startMonitor() {
        TransactionManager transactionManager = TransactionManager.getInstance();
        boolean isExist = transactionManager.existTransactionsForName(TransactionKey);
        if (isExist) {
            return;
        }

        TransactionManager.Transaction transaction = new TransactionManager.Transaction(TransactionKey, 0, 10, new Runnable() {
            @Override
            public void run() {
                monitor();
            }
        });
        transactionManager.addTransaction(transaction);
    }

    // 停止监控
    public synchronized void endMonitor() {
        TransactionManager transactionManager = TransactionManager.getInstance();
        List<TransactionManager.Transaction> transactions = transactionManager.transactionsForName(TransactionKey);
        if (transactions != null) {
            for (TransactionManager.Transaction transaction : transactions) {
                transactionManager.removeTransaction(transaction);
            }
        }
    }

    private void monitor() {
        synchronized (this) {
            if (monitorTaskCount > 0) {
                return;
            }
            monitorTaskCount = 2;
        }

        final ServerConfig serverConfig = cache.getConfig();
        if (serverConfig == null || !serverConfig.isValid()) {
            ServerConfigSynchronizer.getServerConfigFromServer(new ServerConfigSynchronizer.ServerConfigHandler() {
                @Override
                public void handle(ServerConfig config) {
                    if (config == null) {
                        return;
                    }

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
                                GlobalConfiguration.getInstance().udpDnsEnable = udpDnsConfig.getEnable();;
                            }

                            ServerConfig.DnsServer ipv4Servers = udpDnsConfig.getIpv4Server();
                            if (ipv4Servers != null && ipv4Servers.getEnable() != null) {
                                if (ipv4Servers.getEnable()) {
                                    GlobalConfiguration.getInstance().udpDnsIpv4Servers = ipv4Servers.getServers();
                                } else {
                                    GlobalConfiguration.getInstance().udpDnsIpv4Servers = new String[]{};
                                }
                            }

                            ServerConfig.DnsServer ipv6Servers = udpDnsConfig.getIpv6Server();
                            if (ipv6Servers != null && ipv6Servers.getEnable() != null) {
                                if (ipv6Servers.getEnable()) {
                                    GlobalConfiguration.getInstance().udpDnsIpv6Servers = ipv6Servers.getServers();
                                } else {
                                    GlobalConfiguration.getInstance().udpDnsIpv6Servers = new String[]{};
                                }
                            }
                        }

                        // doh 配置
                        ServerConfig.DohDnsConfig dohConfig = dnsConfig.getDohDnsConfig();
                        if (dohConfig != null) {
                            if (dohConfig.getEnable() != null) {
                                GlobalConfiguration.getInstance().dohEnable = dohConfig.getEnable();
                            }

                            ServerConfig.DnsServer ipv4Servers = dohConfig.getIpv4Server();
                            if (ipv4Servers != null && ipv4Servers.getEnable() != null) {
                                if (ipv4Servers.getEnable()) {
                                    GlobalConfiguration.getInstance().dohIpv4Servers = ipv4Servers.getServers();
                                } else {
                                    GlobalConfiguration.getInstance().dohIpv4Servers = new String[]{};
                                }
                            }

                            ServerConfig.DnsServer ipv6Servers = dohConfig.getIpv6Server();
                            if (ipv6Servers != null && ipv6Servers.getEnable() != null) {
                                if (ipv6Servers.getEnable()) {
                                    GlobalConfiguration.getInstance().dohIpv6Servers = ipv6Servers.getServers();
                                } else {
                                    GlobalConfiguration.getInstance().dohIpv6Servers = new String[]{};
                                }
                            }
                        }
                    }

                    cache.setConfig(config);
                    synchronized (ServerConfigMonitor.this) {
                        monitorTaskCount--;
                    }
                }
            });
        }


        ServerUserConfig serverUserConfig = cache.getUserConfig();
        if (serverUserConfig == null || !serverUserConfig.isValid()) {
            ServerConfigSynchronizer.getServerUserConfigFromServer(new ServerConfigSynchronizer.ServerUserConfigHandler() {
                @Override
                public void handle(ServerUserConfig config) {
                    if (config.getNetworkCheckEnable() != null) {
                        GlobalConfiguration.getInstance().connectCheckEnable = config.getNetworkCheckEnable();
                    }
                    cache.setUserConfig(config);
                    synchronized (ServerConfigMonitor.this) {
                        monitorTaskCount--;
                    }
                }
            });
        }
    }
}
