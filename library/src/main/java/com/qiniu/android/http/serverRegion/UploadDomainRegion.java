package com.qiniu.android.http.serverRegion;

import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.dns.DnsPrefetcher;
import com.qiniu.android.http.dns.IDnsNetworkAddress;
import com.qiniu.android.http.networkStatus.UploadServerNetworkStatus;
import com.qiniu.android.http.request.IUploadRegion;
import com.qiniu.android.http.request.IUploadServer;
import com.qiniu.android.http.request.UploadRequestState;
import com.qiniu.android.storage.GlobalConfiguration;
import com.qiniu.android.utils.LogUtil;
import com.qiniu.android.utils.StringUtils;
import com.qiniu.android.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class UploadDomainRegion implements IUploadRegion {
    private final static int Http3FrozenTime = 3600 * 24;

    // 是否支持http3
    private boolean http3Enabled;
    // 是否使用ipv6
    private boolean ipv6Enabled;

    // 是否冻结过Host，PS：如果没有冻结过 Host,则当前 Region 上传也就不会有错误信息，可能会返回-9，所以必须要再进行一次尝试
    private boolean hasFreezeHost;
    private boolean isAllFrozen;
    // 局部冻结管理对象
    private final UploadServerFreezeManager partialHttp2Freezer = new UploadServerFreezeManager();
    private final UploadServerFreezeManager partialHttp3Freezer = new UploadServerFreezeManager();

    private ArrayList<String> domainHostList;
    private HashMap<String, UploadServerDomain> domainHashMap;
    private ArrayList<String> oldDomainHostList;
    private HashMap<String, UploadServerDomain> oldDomainHashMap;
    private ZoneInfo zoneInfo;

    @Override
    public boolean isEqual(IUploadRegion region) {
        if (region == null) {
            return false;
        }

        if (region.getZoneInfo() == null && getZoneInfo() == null) {
            return true;
        }

        if (region.getZoneInfo() == null || getZoneInfo() == null) {
            return false;
        }

        if (region.getZoneInfo().getRegionId() == null && getZoneInfo().getRegionId() == null) {
            return true;
        }

        if (region.getZoneInfo().getRegionId() == null || getZoneInfo().getRegionId() == null) {
            return false;
        }

        if (region.getZoneInfo().getRegionId().equals(getZoneInfo().getRegionId())) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isValid() {
        return !isAllFrozen && (domainHostList.size() > 0 || oldDomainHostList.size() > 0);
    }

    @Override
    public ZoneInfo getZoneInfo() {
        return zoneInfo;
    }

    @Override
    public void setupRegionData(ZoneInfo zoneInfo) {
        if (zoneInfo == null) {
            return;
        }

        this.zoneInfo = zoneInfo;

        isAllFrozen = false;

        ipv6Enabled = zoneInfo.ipv6;

        ArrayList<String> domainHostList = new ArrayList<>();
        if (zoneInfo.domains != null) {
            domainHostList.addAll(zoneInfo.domains);
        }
        this.domainHostList = domainHostList;
        this.domainHashMap = createDomainDictionary(domainHostList);

        ArrayList<String> oldDomainHostList = new ArrayList<>();
        if (zoneInfo.old_domains != null) {
            oldDomainHostList.addAll(zoneInfo.old_domains);
        }
        this.oldDomainHostList = oldDomainHostList;
        this.oldDomainHashMap = createDomainDictionary(oldDomainHostList);

        LogUtil.i("region :" + StringUtils.toNonnullString(domainHostList));
        LogUtil.i("region old:" + StringUtils.toNonnullString(oldDomainHostList));
    }

    private HashMap<String, UploadServerDomain> createDomainDictionary(List<String> hosts) {
        HashMap<String, UploadServerDomain> domainHashMap = new HashMap<>();
        for (int i = 0; i < hosts.size(); i++) {
            String host = hosts.get(i);
            UploadServerDomain domain = new UploadServerDomain(host);
            domainHashMap.put(host, domain);
        }
        return domainHashMap;
    }

    @Override
    public IUploadServer getNextServer(UploadRequestState requestState, ResponseInfo responseInfo, IUploadServer freezeServer) {
        if (isAllFrozen || requestState == null) {
            return null;
        }

        freezeServerIfNeed(responseInfo, freezeServer);

        ArrayList<String> hostList = domainHostList;
        HashMap<String, UploadServerDomain> domainInfo = domainHashMap;
        if (requestState.isUseOldServer()
                && oldDomainHostList != null && oldDomainHostList.size() > 0
                && oldDomainHashMap != null && oldDomainHashMap.size() > 0) {
            hostList = oldDomainHostList;
            domainInfo = oldDomainHashMap;
        }

        // 1. 优先选择http3
        UploadServer http3Server = null;
        if (couldUseHttp3() && requestState.couldUseHttp3()) {
            for (String host : hostList) {
                UploadServerDomain domain = domainInfo.get(host);
                if (domain == null) {
                    continue;
                }
                UploadServer domainServer = domain.getServer(new UploadServerDomain.GetServerCondition() {
                    @Override
                    public boolean condition(String host, UploadServer serverP, UploadServer filterServer) {
                        String filterServerIP = filterServer == null ? null : filterServer.getIp();

                        // 1.1 剔除 ipv6
                        if (!ipv6Enabled && Utils.isIpv6(filterServerIP)) {
                            return false;
                        }

                        // 1.2 剔除冻结对象
                        String frozenType = UploadServerFreezeUtil.getFrozenType(host, filterServerIP);
                        boolean isFrozen = UploadServerFreezeUtil.isTypeFrozenByFreezeManagers(frozenType, new UploadServerFreezeManager[]{partialHttp3Freezer, UploadServerFreezeUtil.globalHttp3Freezer()});

                        if (isFrozen || !HttpServerManager.getInstance().isServerSupportHttp3(host, filterServerIP)) {
                            return false;
                        }

                        // 1.3 挑选网络状态最优
                        return UploadServerNetworkStatus.isServerNetworkBetter(filterServer, serverP);
                    }
                });
                if (domainServer != null) {
                    domainServer.setHttpVersion(IUploadServer.HttpVersion3);
                }

                http3Server = (UploadServer) UploadServerNetworkStatus.getBetterNetworkServer(domainServer, http3Server);

                if (http3Server != null) {
                    break;
                }
            }
        }

        // 2. 挑选http2
        UploadServer http2Server = null;
        for (String host : hostList) {
            UploadServerDomain domain = domainInfo.get(host);
            if (domain == null) {
                continue;
            }
            UploadServer domainServer = domain.getServer(new UploadServerDomain.GetServerCondition() {
                @Override
                public boolean condition(String host, UploadServer serverP, UploadServer filterServer) {
                    String filterServerIP = filterServer == null ? null : filterServer.getIp();

                    // 1.1 剔除 ipv6
                    if (!ipv6Enabled && Utils.isIpv6(filterServerIP)) {
                        return false;
                    }

                    // 1.2 剔除冻结对象
                    String frozenType = UploadServerFreezeUtil.getFrozenType(host, filterServerIP);
                    boolean isFrozen = UploadServerFreezeUtil.isTypeFrozenByFreezeManagers(frozenType, new UploadServerFreezeManager[]{partialHttp2Freezer, UploadServerFreezeUtil.globalHttp2Freezer()});

                    if (isFrozen) {
                        return false;
                    }

                    // 1.3 挑选网络状态最优
                    return UploadServerNetworkStatus.isServerNetworkBetter(filterServer, serverP);
                }
            });
            if (domainServer != null) {
                domainServer.setHttpVersion(IUploadServer.HttpVersion2);
            }

            http2Server = (UploadServer) UploadServerNetworkStatus.getBetterNetworkServer(domainServer, http2Server);

            if (http2Server != null) {
                break;
            }
        }

        UploadServer server = (UploadServer) UploadServerNetworkStatus.getBetterNetworkServer(http3Server, http2Server);
        if (server == null && !hasFreezeHost && hostList.size() > 0) {
            int index = (int) (Math.random() * hostList.size());
            String host = hostList.get(index);
            UploadServerDomain domain = domainInfo.get(host);
            if (domain != null) {
                server = domain.getOneServer();
                if (server != null) {
                    server.setHttpVersion(IUploadServer.HttpVersion2);
                }
            }
            unfreezeServer(server);
        }

        if (server != null) {
            LogUtil.i("get server host:" + StringUtils.toNonnullString(server.getHost()) + " ip:" + StringUtils.toNonnullString(server.getIp()));
        } else {
            isAllFrozen = true;
            LogUtil.i("get server host:null ip:null");
        }

        return server;
    }

    @Override
    public void updateIpListFormHost(String host) {
        if (host == null) {
            return;
        }

        if (domainHashMap != null && domainHashMap.get(host) != null) {
            UploadServerDomain domain = domainHashMap.get(host);
            if (domain != null) {
                domain.clearIpGroupList();
            }
        }

        if (oldDomainHashMap != null && oldDomainHashMap.get(host) != null) {
            UploadServerDomain domain = oldDomainHashMap.get(host);
            if (domain != null) {
                domain.clearIpGroupList();
            }
        }
    }

    private void freezeServerIfNeed(ResponseInfo responseInfo, IUploadServer freezeServer) {
        if (responseInfo == null || freezeServer == null || freezeServer.getServerId() == null) {
            return;
        }

        String frozenType = UploadServerFreezeUtil.getFrozenType(freezeServer.getHost(), freezeServer.getIp());
        // 1. http3 冻结
        if (freezeServer.isHttp3()) {
            if (responseInfo.isNotQiniu() || !responseInfo.canConnectToHost() || responseInfo.isHostUnavailable()) {
                hasFreezeHost = true;
                partialHttp3Freezer.freezeType(frozenType, GlobalConfiguration.getInstance().partialHostFrozenTime);
            }

            if (IUploadServer.HttpVersion3.equals(responseInfo.httpVersion) || responseInfo.isHostUnavailable()) {
                hasFreezeHost = true;
                UploadServerFreezeUtil.globalHttp3Freezer().freezeType(frozenType, GlobalConfiguration.getInstance().globalHostFrozenTime);
            }
            return;
        }

        // 2. http2 冻结
        // 2.1 无法连接到Host || Host不可用， 局部冻结
        if (responseInfo.isNotQiniu() || !responseInfo.canConnectToHost() || responseInfo.isHostUnavailable()) {
            hasFreezeHost = true;
            LogUtil.i("partial freeze server host:" + StringUtils.toNonnullString(freezeServer.getHost()) + " ip:" + StringUtils.toNonnullString(freezeServer.getIp()));
            partialHttp2Freezer.freezeType(frozenType, GlobalConfiguration.getInstance().partialHostFrozenTime);
        }

        // 2.2 Host不可用，全局冻结
        if (responseInfo.isHostUnavailable()) {
            hasFreezeHost = true;
            LogUtil.i("global freeze server host:" + StringUtils.toNonnullString(freezeServer.getHost()) + " ip:" + StringUtils.toNonnullString(freezeServer.getIp()));
            UploadServerFreezeUtil.globalHttp2Freezer().freezeType(frozenType, GlobalConfiguration.getInstance().globalHostFrozenTime);
        }
    }

    private void unfreezeServer(IUploadServer freezeServer) {
        if (freezeServer == null || freezeServer.getServerId() == null) {
            return;
        }

        String frozenType = UploadServerFreezeUtil.getFrozenType(freezeServer.getHost(), freezeServer.getIp());
        partialHttp2Freezer.unfreezeType(frozenType);
        partialHttp3Freezer.unfreezeType(frozenType);
    }

    private boolean couldUseHttp3() {
        return GlobalConfiguration.getInstance().enableHttp3;
    }


    private static class UploadServerDomain {

        protected final String host;
        protected ArrayList<UploadIpGroup> ipGroupList = new ArrayList<>();

        protected UploadServerDomain(String host) {
            this.host = host;
        }

        protected UploadServer getServer(GetServerCondition condition) {
            if (host == null || host.length() == 0) {
                return null;
            }

            List<UploadIpGroup> ipGroups = null;
            synchronized (this) {
                if (ipGroupList == null || ipGroupList.size() == 0) {
                    createIpGroupList();
                }
                ipGroups = ipGroupList;
            }

            UploadServer server = null;
            // 解析到IP:
            if (ipGroups != null && ipGroups.size() > 0) {
                for (UploadIpGroup ipGroup : ipGroups) {
                    IDnsNetworkAddress networkAddress = ipGroup.getNetworkAddress();
                    UploadServer filterServer = new UploadServer(host, host, networkAddress.getIpValue(), networkAddress.getSourceValue(), networkAddress.getTimestampValue());

                    if (condition == null || condition.condition(host, server, filterServer)) {
                        server = filterServer;
                    }

                    if (condition == null) {
                        break;
                    }
                }

                return server;
            }

            // 未解析到IP:
            UploadServer hostServer = new UploadServer(host, host, null, null, null);
            if (condition == null || condition.condition(host, null, hostServer)) {
                // 未解析时，没有可比性，直接返回自身，自身即为最优
                server = hostServer;
            }

            return server;
        }

        protected UploadServer getOneServer() {
            if (host == null || host.length() == 0) {
                return null;
            }

            List<UploadIpGroup> ipGroups = null;
            synchronized (this) {
                ipGroups = ipGroupList;
            }

            if (ipGroups != null && ipGroups.size() > 0) {
                int index = (int) (Math.random() * ipGroups.size());
                UploadIpGroup ipGroup = ipGroups.get(index);
                IDnsNetworkAddress inetAddress = ipGroup.getNetworkAddress();
                UploadServer server = new UploadServer(host, host, inetAddress.getIpValue(), inetAddress.getSourceValue(), inetAddress.getTimestampValue());
                return server;
            } else {
                return new UploadServer(host, host, null, null, null);
            }
        }

        synchronized void clearIpGroupList() {
            ipGroupList = null;
        }

        private void createIpGroupList() {
            if (ipGroupList != null && ipGroupList.size() > 0) {
                return;
            }

            List<IDnsNetworkAddress> networkAddresses = DnsPrefetcher.getInstance().getInetAddressByHost(host);
            if (networkAddresses == null || networkAddresses.size() == 0) {
                return;
            }

            HashMap<String, ArrayList<IDnsNetworkAddress>> ipGroupInfo = new HashMap<>();
            for (IDnsNetworkAddress networkAddress : networkAddresses) {
                String ipValue = networkAddress.getIpValue();
                if (ipValue == null) {
                    continue;
                }
                String groupType = Utils.getIpType(ipValue, host);
                if (groupType != null) {
                    ArrayList<IDnsNetworkAddress> groupNetworkAddresses = ipGroupInfo.get(groupType);
                    if (groupNetworkAddresses == null) {
                        groupNetworkAddresses = new ArrayList<>();
                    }
                    groupNetworkAddresses.add(networkAddress);
                    ipGroupInfo.put(groupType, groupNetworkAddresses);
                }
            }

            ArrayList<UploadIpGroup> ipGroupList = new ArrayList<>();
            for (String groupType : ipGroupInfo.keySet()) {
                ArrayList<IDnsNetworkAddress> addresses = ipGroupInfo.get(groupType);
                UploadIpGroup ipGroup = new UploadIpGroup(groupType, addresses);
                ipGroupList.add(ipGroup);
            }
            this.ipGroupList = ipGroupList;
        }

        protected interface GetServerCondition {
            boolean condition(String host, UploadServer server, UploadServer filterServer);
        }
    }

    private static class UploadIpGroup {
        private int addressIndex = -1;
        private final String groupType;
        private final ArrayList<IDnsNetworkAddress> addressList;

        protected UploadIpGroup(String groupType,
                                ArrayList<IDnsNetworkAddress> addressList) {
            this.groupType = groupType;
            this.addressList = addressList;
        }

        protected IDnsNetworkAddress getNetworkAddress() {
            if (addressList == null || addressList.size() == 0) {
                return null;
            } else {
                if (addressIndex < 0 || addressIndex > (addressList.size() - 1)) {
                    addressIndex = (int) (Math.random() * addressList.size());
                }
                return addressList.get(addressIndex);
            }
        }
    }

}
