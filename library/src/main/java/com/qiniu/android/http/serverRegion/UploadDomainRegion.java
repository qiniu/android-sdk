package com.qiniu.android.http.serverRegion;

import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.dns.DnsPrefetcher;
import com.qiniu.android.http.dns.IDnsNetworkAddress;
import com.qiniu.android.http.networkCheck.NetworkCheckManager;
import com.qiniu.android.http.networkCheck.NetworkCheckTransaction;
import com.qiniu.android.http.request.IUploadRegion;
import com.qiniu.android.http.request.IUploadServer;
import com.qiniu.android.storage.GlobalConfiguration;
import com.qiniu.android.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class UploadDomainRegion implements IUploadRegion {

    // 是否获取过，PS：当第一次获取Domain，而区域所有Domain又全部冻结时，返回一个domain尝试一次
    private boolean hasGot;
    private boolean isAllFrozen;
    // 局部冻结管理对象
    private UploadServerFreezeManager partialFreezeManager = new UploadServerFreezeManager();

    private ArrayList<String> domainHostList;
    private HashMap<String, UploadServerDomain> domainHashMap;
    private ArrayList<String> oldDomainHostList;
    private HashMap<String, UploadServerDomain> oldDomainHashMap;
    private ZoneInfo zoneInfo;

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
    }

    @Override
    public IUploadServer getNextServer(boolean isOldServer, ResponseInfo responseInfo, IUploadServer freezeServer) {
        if (isAllFrozen) {
            return null;
        }

        freezeServerIfNeed(responseInfo, freezeServer);

        ArrayList<String> hostList = isOldServer ? oldDomainHostList : domainHostList;
        HashMap<String, UploadServerDomain> domainInfo = isOldServer ? oldDomainHashMap : domainHashMap;
        IUploadServer server = null;
        for (String host : hostList) {
            UploadServerDomain domain = domainInfo.get(host);
            if (domain != null) {
                server = domain.getServer(new UploadServerFreezeManager[]{partialFreezeManager, UploadServerFreezeManager.getInstance()});
            }
            if (server != null) {
                break;
            }
        }

        if (server == null && !hasGot && hostList.size() > 0) {
            int index = (int) (Math.random() * hostList.size());
            String host = hostList.get(index);
            UploadServerDomain domain = domainInfo.get(host);
            if (domain != null) {
                server = domain.getOneServer();
            }
            unfreezeServer(server);
        }
        hasGot = true;

        if (server == null) {
            isAllFrozen = true;
        }

        return server;
    }


    private void freezeServerIfNeed(ResponseInfo responseInfo, IUploadServer freezeServer) {
        if (responseInfo == null || freezeServer == null || freezeServer.getServerId() == null) {
            return;
        }

        // 无法连接到Host || Host不可用， 局部冻结
        if (!responseInfo.canConnectToHost() || responseInfo.isHostUnavailable()) {
            UploadServerDomain domain = null;
            domain = domainHashMap.get(freezeServer.getServerId());
            if (domain != null) {
                domain.freeze(freezeServer.getIp(), partialFreezeManager, GlobalConfiguration.getInstance().partialHostFrozenTime);
            }
            domain = oldDomainHashMap.get(freezeServer.getServerId());
            if (domain != null) {
                domain.freeze(freezeServer.getIp(), partialFreezeManager, GlobalConfiguration.getInstance().partialHostFrozenTime);
            }
        }

        // Host不可用，全局冻结
        if (responseInfo.isHostUnavailable()) {
            UploadServerDomain domain = null;
            domain = domainHashMap.get(freezeServer.getServerId());
            if (domain != null) {
                domain.freeze(freezeServer.getIp(), UploadServerFreezeManager.getInstance(), GlobalConfiguration.getInstance().globalHostFrozenTime);
            }
            domain = oldDomainHashMap.get(freezeServer.getServerId());
            if (domain != null) {
                domain.freeze(freezeServer.getIp(), UploadServerFreezeManager.getInstance(), GlobalConfiguration.getInstance().globalHostFrozenTime);
            }
        }
    }

    private void unfreezeServer(IUploadServer freezeServer) {
        if (freezeServer == null || freezeServer.getServerId() == null) {
            return;
        }

        UploadServerDomain domain = null;
        domain = domainHashMap.get(freezeServer.getServerId());
        if (domain != null) {
            domain.unfreeze(freezeServer.getIp(), new UploadServerFreezeManager[]{partialFreezeManager, UploadServerFreezeManager.getInstance()});
        }
        domain = oldDomainHashMap.get(freezeServer.getServerId());
        if (domain != null) {
            domain.unfreeze(freezeServer.getIp(), new UploadServerFreezeManager[]{partialFreezeManager, UploadServerFreezeManager.getInstance()});
        }
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


    private static class UploadServerDomain {

        private boolean isAllFrozen = false;
        protected final String host;
        protected ArrayList<UploadIpGroup> ipGroupList = new ArrayList<>();

        protected UploadServerDomain(String host) {
            this.host = host;
        }

        protected IUploadServer getServer(UploadServerFreezeManager[] freezeManagerList) {
            if (isAllFrozen || host == null || host.length() == 0) {
                return null;
            }

            if (ipGroupList == null || ipGroupList.size() == 0) {
                createIpGroupList();
            }

            // 解析到IP:
            if (ipGroupList != null && ipGroupList.size() > 0) {
                UploadServer server = null;
                for (UploadIpGroup ipGroup : ipGroupList) {
                    // 黑名单中不存在 & 未被冻结
                    if (ipGroup.groupType != null && !isGroupFrozenByFreezeManagers(ipGroup.groupType, freezeManagerList)) {
                        IDnsNetworkAddress networkAddress = ipGroup.getNetworkAddress();
                        server = new UploadServer(host, host, networkAddress.getIpValue(), networkAddress.getSourceValue(), networkAddress.getTimestampValue());
                        break;
                    }
                }
                if (server == null) {
                    isAllFrozen = true;
                }
                return server;
            }

            // 未解析到IP:
            // 黑名单中不存在 & 未被冻结
            String groupType = Utils.getIpType(null, host);
            if (groupType != null && !isGroupFrozenByFreezeManagers(groupType, freezeManagerList)) {
                return new UploadServer(host, host, null, null, null);
            } else {
                isAllFrozen = true;
                return null;
            }
        }

        protected boolean isGroupFrozenByFreezeManagers(String groupType, UploadServerFreezeManager[] freezeManagerList) {
            if (groupType == null) {
                return true;
            }
            if (freezeManagerList == null || freezeManagerList.length == 0) {
                return false;
            }

            boolean isFrozen = false;
            for (UploadServerFreezeManager freezeManager : freezeManagerList) {
                isFrozen = freezeManager.isFreezeHost(host, groupType);
                if (isFrozen) {
                    break;
                }
            }
            return isFrozen;
        }

        protected UploadServer getOneServer() {
            if (host == null || host.length() == 0) {
                return null;
            }
            if (ipGroupList != null && ipGroupList.size() > 0) {
                int index = (int) (Math.random() * ipGroupList.size());
                UploadIpGroup ipGroup = ipGroupList.get(index);
                IDnsNetworkAddress inetAddress = ipGroup.getNetworkAddress();
                UploadServer server = new UploadServer(host, host, inetAddress.getIpValue(), inetAddress.getSourceValue(), inetAddress.getTimestampValue());
                return server;
            } else {
                return new UploadServer(host, host, null, null, null);
            }
        }

        private synchronized void createIpGroupList() {
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

        protected void freeze(String ip, UploadServerFreezeManager freezeManager, int frozenTime) {
            if (freezeManager == null) {
                return;
            }
            freezeManager.freezeHost(host, Utils.getIpType(ip, this.host), frozenTime);
        }

        protected void unfreeze(String ip, UploadServerFreezeManager[] freezeManagerList) {
            if (freezeManagerList == null || freezeManagerList.length == 0) {
                return;
            }
            for (UploadServerFreezeManager freezeManager : freezeManagerList) {
                freezeManager.unfreezeHost(host, Utils.getIpType(ip, this.host));
            }
        }
    }

    private static class UploadIpGroup {
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
                int index = (int) (Math.random() * addressList.size());
                return addressList.get(index);
            }
        }

    }

}
