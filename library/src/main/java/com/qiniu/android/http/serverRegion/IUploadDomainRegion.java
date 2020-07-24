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


public class IUploadDomainRegion implements IUploadRegion {

    private boolean isAllFrozen;
    private ArrayList<String> domainHostList;
    private HashMap<String, UploadServerDomain> domainHashMap;
    private ArrayList<String> oldDomainHostList;
    private HashMap<String, UploadServerDomain> oldDomainHashMap;
    private ZoneInfo zoneInfo;

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
        ArrayList<ZoneInfo.UploadServerGroup> serverGroups = new ArrayList<>();
        if (zoneInfo.acc != null){
            serverGroups.add(zoneInfo.acc);
            if (zoneInfo.acc.allHosts != null){
                domainHostList.addAll(zoneInfo.acc.allHosts);
            }
        }
        if (zoneInfo.src != null){
            serverGroups.add(zoneInfo.src);
            if (zoneInfo.src.allHosts != null){
                domainHostList.addAll(zoneInfo.src.allHosts);
            }
        }
        this.domainHostList = domainHostList;
        domainHashMap = createDomainDictionary(serverGroups);

        ArrayList<String> oldDomainHostList = new ArrayList<>();
        serverGroups = new ArrayList<>();
        if (zoneInfo.old_acc != null){
            serverGroups.add(zoneInfo.old_acc);
            if (zoneInfo.old_acc.allHosts != null){
                oldDomainHostList.addAll(zoneInfo.old_acc.allHosts);
            }
        }
        if (zoneInfo.old_src != null){
            serverGroups.add(zoneInfo.old_src);
            if (zoneInfo.old_src.allHosts != null){
                oldDomainHostList.addAll(zoneInfo.old_src.allHosts);
            }
        }
        this.oldDomainHostList = oldDomainHostList;
        oldDomainHashMap = createDomainDictionary(serverGroups);
    }

    @Override
    public IUploadServer getNextServer(boolean isOldServer, IUploadServer freezeServer) {
        if (isAllFrozen){
            return null;
        }
        if (freezeServer != null && freezeServer.getServerId() != null){
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

        ArrayList<String> hostList = isOldServer ? oldDomainHostList : domainHostList;
        HashMap<String, UploadServerDomain> domainInfo = isOldServer ? oldDomainHashMap : domainHashMap;
        IUploadServer server = null;
        for (String host : hostList) {
            UploadServerDomain domain = domainInfo.get(host);
            if (domain != null){
                server =  domain.getServer();
            }
            if (server != null){
                break;
            }
        }

        if (server == null){
            isAllFrozen = true;
        }

        return server;
    }

    private HashMap<String, UploadServerDomain> createDomainDictionary(ArrayList<ZoneInfo.UploadServerGroup> serverGroups){
        HashMap<String, UploadServerDomain> domainHashMap = new HashMap<>();
        for (int i = 0; i < serverGroups.size(); i++) {
            ZoneInfo.UploadServerGroup serverGroup = serverGroups.get(i);
            for (int j = 0; j < serverGroup.allHosts.size(); j++){
                String host = serverGroup.allHosts.get(j);
                UploadServerDomain domain = new UploadServerDomain(host);
                domainHashMap.put(host, domain);
            }
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

        protected IUploadServer getServer(){
            if (isAllFrozen || host == null || host.length() == 0){
                return null;
            }

            if (ipGroupList == null || ipGroupList.size() == 0){
                createIpGroupList();
            }

            if (ipGroupList != null && ipGroupList.size() > 0){
                UploadServer server = null;
                for (UploadIpGroup ipGroup : ipGroupList){
                    if (!UploadServerFreezeManager.getInstance().isFreezeHost(host, ipGroup.groupType)){
                        IDnsNetworkAddress networkAddress = ipGroup.getNetworkAddress();
                        server = new UploadServer(host, host, networkAddress.getIpValue(), networkAddress.getSourceValue(), networkAddress.getTimestampValue());
                        break;
                    }
                }
                if (server == null){
                    isAllFrozen = true;
                }
                return server;
            } else if (!UploadServerFreezeManager.getInstance().isFreezeHost(host, null)){
                return new UploadServer(host, host, null, null, null);
            } else {
                isAllFrozen = true;
                return null;
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
