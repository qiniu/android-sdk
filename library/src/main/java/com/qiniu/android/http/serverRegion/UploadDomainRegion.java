package com.qiniu.android.http.serverRegion;

import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.http.dns.DnsPrefetcher;
import com.qiniu.android.http.dns.IDnsNetworkAddress;
import com.qiniu.android.http.request.IUploadRegion;
import com.qiniu.android.http.request.IUploadServer;
import com.qiniu.android.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class UploadDomainRegion implements IUploadRegion {

    // 是否获取过，PS：当第一次获取Domain，而区域所有Domain又全部冻结时，返回一个domain尝试一次
    private boolean hasGot;
    private boolean isAllFrozen;
    // server黑名单，保证使用过的server不再使用，防止无限重试
    private HashMap<String, IUploadServer> blackServerInfo;
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
        blackServerInfo = new HashMap<>();

        ArrayList<String> domainHostList = new ArrayList<>();
        if (zoneInfo.domains != null){
            domainHostList.addAll(zoneInfo.domains);
        }
        this.domainHostList = domainHostList;
        this.domainHashMap = createDomainDictionary(domainHostList);

        ArrayList<String> oldDomainHostList = new ArrayList<>();
        if (zoneInfo.old_domains != null){
            oldDomainHostList.addAll(zoneInfo.old_domains);
        }
        this.oldDomainHostList = domainHostList;
        this.oldDomainHashMap = createDomainDictionary(domainHostList);
    }

    @Override
    public IUploadServer getNextServer(boolean isOldServer, int frozenLevel, IUploadServer freezeServer) {
        if (isAllFrozen){
            return null;
        }
        if (freezeServer != null && freezeServer.getServerId() != null){

            if ((frozenLevel & IUploadRegion.FrozenLevelPartFrozen) == IUploadRegion.FrozenLevelPartFrozen){
                String ipType = Utils.getIpType(freezeServer.getIp(), freezeServer.getHost());
                if (ipType != null && ipType.length() > 0 && blackServerInfo != null){
                    blackServerInfo.put(ipType, freezeServer);
                }
            }

            if ((frozenLevel & IUploadRegion.FrozenLevelGlobalFrozen) == IUploadRegion.FrozenLevelGlobalFrozen){
                UploadServerDomain domain = null;
                domain = domainHashMap.get(freezeServer.getServerId());
                if (domain != null){
                    domain.freeze(freezeServer.getIp());
                }
                domain = oldDomainHashMap.get(freezeServer.getServerId());
                if (domain != null){
                    domain.freeze(freezeServer.getIp());
                }
            }
        }

        ArrayList<String> hostList = isOldServer ? oldDomainHostList : domainHostList;
        HashMap<String, UploadServerDomain> domainInfo = isOldServer ? oldDomainHashMap : domainHashMap;
        IUploadServer server = null;
        for (String host : hostList) {
            UploadServerDomain domain = domainInfo.get(host);
            if (domain != null){
                server = domain.getServer(blackServerInfo);
            }
            if (server != null){
                break;
            }
        }

        if (server == null && !hasGot && hostList.size() > 0){
            int index = (int)(Math.random() * hostList.size());
            String host = hostList.get(index);
            UploadServerDomain domain = domainInfo.get(host);
            if (domain != null){
                server = domain.getOneServer();
            }
        }
        hasGot = true;

        if (server == null){
            isAllFrozen = true;
        }

        return server;
    }

    private HashMap<String, UploadServerDomain> createDomainDictionary(List<String> hosts){
        HashMap<String, UploadServerDomain> domainHashMap = new HashMap<>();
        for (int i = 0; i < hosts.size(); i++) {
            String host = hosts.get(i);
            UploadServerDomain domain = new UploadServerDomain(host);
            domainHashMap.put(host, domain);
        }
        return  domainHashMap;
    }


    private static class UploadServerDomain{

        private boolean isAllFrozen = false;
        protected final String host;
        protected ArrayList<UploadIpGroup> ipGroupList = new ArrayList<>();

        protected UploadServerDomain(String host){
            this.host = host;
        }

        protected IUploadServer getServer(Map<String, IUploadServer> blackServerInfo){
            if (isAllFrozen || host == null || host.length() == 0){
                return null;
            }

            if (ipGroupList == null || ipGroupList.size() == 0){
                createIpGroupList();
            }

            // 解析到IP:
            if (ipGroupList != null && ipGroupList.size() > 0){
                UploadServer server = null;
                for (UploadIpGroup ipGroup : ipGroupList){
                    // 黑名单中不存在 & 未被冻结
                    if (ipGroup.groupType != null
                            && (blackServerInfo == null || blackServerInfo.get(ipGroup.groupType) == null)
                            && !UploadServerFreezeManager.getInstance().isFreezeHost(host, ipGroup.groupType)){
                        IDnsNetworkAddress networkAddress = ipGroup.getNetworkAddress();
                        server = new UploadServer(host, host, networkAddress.getIpValue(), networkAddress.getSourceValue(), networkAddress.getTimestampValue());
                        break;
                    }
                }
                if (server == null){
                    isAllFrozen = true;
                }
                return server;
            }

            // 未解析到IP:
            // 黑名单中不存在 & 未被冻结
            String groupType = Utils.getIpType(null, host);
            if (groupType != null
                    && (blackServerInfo == null || blackServerInfo.get(groupType) == null)
                    && !UploadServerFreezeManager.getInstance().isFreezeHost(host, null)){
                return new UploadServer(host, host, null, null, null);
            } else {
                isAllFrozen = true;
                return null;
            }
        }

        protected UploadServer getOneServer(){
            if (host == null || host.length() == 0) {
                return null;
            }
            if (ipGroupList != null && ipGroupList.size() > 0) {
                int index = (int)(Math.random() * ipGroupList.size());
                UploadIpGroup ipGroup = ipGroupList.get(index);
                IDnsNetworkAddress inetAddress = ipGroup.getNetworkAddress();
                UploadServer server = new UploadServer(host, host, inetAddress.getIpValue(), inetAddress.getSourceValue(), inetAddress.getTimestampValue());
                return server;
            } else {
                return new UploadServer(host, host, null, null, null);
            }
        }

        private synchronized void createIpGroupList(){
           if (ipGroupList != null && ipGroupList.size() > 0){
               return;
           }

           List<IDnsNetworkAddress> networkAddresses = DnsPrefetcher.getInstance().getInetAddressByHost(host);
           if (networkAddresses == null || networkAddresses.size() == 0){
               return;
           }

           HashMap<String, ArrayList<IDnsNetworkAddress>> ipGroupInfo = new HashMap<>();
           for (IDnsNetworkAddress networkAddress : networkAddresses){
               String ipValue = networkAddress.getIpValue();
               if (ipValue == null){
                   continue;
               }
               String groupType = Utils.getIpType(ipValue, host);
               if (groupType != null){
                   ArrayList<IDnsNetworkAddress> groupNetworkAddresses = ipGroupInfo.get(groupType);
                   if (groupNetworkAddresses == null) {
                       groupNetworkAddresses = new ArrayList<>();
                   }
                   groupNetworkAddresses.add(networkAddress);
                   ipGroupInfo.put(groupType, groupNetworkAddresses);
               }
           }

           ArrayList<UploadIpGroup> ipGroupList = new ArrayList<>();
           for (String groupType : ipGroupInfo.keySet()){
               ArrayList<IDnsNetworkAddress> addresses = ipGroupInfo.get(groupType);
               UploadIpGroup ipGroup = new UploadIpGroup(groupType, addresses);
               ipGroupList.add(ipGroup);
           }
           this.ipGroupList = ipGroupList;
        }

        protected void freeze(String ip){
            UploadServerFreezeManager.getInstance().freezeHost(host, Utils.getIpType(ip, this.host));
        }
    }

    private static class UploadIpGroup{
        private final String groupType;
        private final ArrayList<IDnsNetworkAddress> addressList;

        protected UploadIpGroup(String groupType,
                                ArrayList<IDnsNetworkAddress> addressList) {
            this.groupType = groupType;
            this.addressList = addressList;
        }

        protected IDnsNetworkAddress getNetworkAddress(){
            if (addressList == null || addressList.size() == 0){
                return null;
            } else {
                int index = (int)(Math.random()*addressList.size());
                return addressList.get(index);
            }
        }

    }

}
