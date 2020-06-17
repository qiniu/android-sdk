package com.qiniu.android.http.request.serverRegion;

import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.http.dns.DnsPrefetcher;
import com.qiniu.android.http.request.UploadRegion;
import com.qiniu.android.http.request.UploadServerInterface;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UploadDomainRegion implements UploadRegion {

    private boolean isAllFreezed;
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

        isAllFreezed = false;
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
    public UploadServerInterface getNextServer(boolean isOldServer, UploadServerInterface freezeServer) {
        if (isAllFreezed){
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
        UploadServerInterface server = null;
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
            isAllFreezed = true;
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

        private boolean isAllFreezed = false;
        protected final String host;
        protected ArrayList<UploadIpGroup> ipGroupList = new ArrayList<>();

        protected UploadServerDomain(String host){
            this.host = host;
        }

        protected UploadServerInterface getServer(){
            if (isAllFreezed || host == null || host.length() == 0){
                return null;
            }

            if (ipGroupList == null || ipGroupList.size() == 0){
                createIpGroupList();
            }

            if (ipGroupList != null && ipGroupList.size() > 0){
                UploadServer server = null;
                for (UploadIpGroup ipGroup : ipGroupList){
                    if (!UploadServerFreezeManager.getInstance().isFreezeHost(host, ipGroup.groupType)){
                        server = new UploadServer(host, host, ipGroup.getInetAddress());
                        break;
                    }
                }
                if (server == null){
                    isAllFreezed = true;
                }
                return server;
            } else if (!UploadServerFreezeManager.getInstance().isFreezeHost(host, null)){
                return new UploadServer(host, host, null);
            } else {
                isAllFreezed = true;
                return null;
            }
        }

        private synchronized void createIpGroupList(){
           if (ipGroupList != null && ipGroupList.size() > 0){
               return;
           }

           List<InetAddress> inetAddresses = DnsPrefetcher.getInstance().getInetAddressByHost(host);
           if (inetAddresses == null || inetAddresses.size() == 0){
               return;
           }

           HashMap<String, ArrayList<InetAddress>> ipGroupInfos = new HashMap<>();
           for (InetAddress inetAddress : inetAddresses){
               String ipValue = inetAddress.getHostAddress();
               if (ipValue == null){
                   continue;
               }
               String groupType = getIpType(ipValue);
               if (groupType != null){
                   ArrayList<InetAddress> inetAddressArrayList = ipGroupInfos.get(groupType);
                   if (inetAddressArrayList == null) {
                       inetAddressArrayList = new ArrayList<>();
                   }
                   inetAddressArrayList.add(inetAddress);
                   ipGroupInfos.put(groupType, inetAddressArrayList);
               }
           }

           ArrayList<UploadIpGroup> ipGroupList = new ArrayList<>();
           for (String groupType : ipGroupInfos.keySet()){
               ArrayList<InetAddress> inetAddressList = ipGroupInfos.get(groupType);
               UploadIpGroup ipGroup = new UploadIpGroup(groupType, inetAddressList);
               ipGroupList.add(ipGroup);
           }
           this.ipGroupList = ipGroupList;
        }

        protected void freeze(String ip){
            UploadServerFreezeManager.getInstance().freezeHost(host, getIpType(ip));
        }

        private String getIpType(String ip){
            String type = null;
            if (ip == null || ip.length() == 0) {
                return type;
            }
            if (ip.contains(":")) {
                type = getIPV6StringType(ip);
            } else if (ip.contains(".")){
                type = getIPV4StringType(ip);
            }
            return type;
        }

        private String getIPV4StringType(String ipv4String){
            String type = null;
            String[] ipNumbers = ipv4String.split("\\.");
            if (ipNumbers.length == 4){
                int firstNumber = Integer.parseInt(ipNumbers[0]);
                if (firstNumber > 0 && firstNumber < 127) {
                    type = "ipv4-A-" + firstNumber;
                } else if (firstNumber > 127 && firstNumber <= 191) {
                    type = "ipv4-B-" + firstNumber + ipNumbers[1];
                } else if (firstNumber > 191 && firstNumber <= 223) {
                    type = "ipv4-C-"+ firstNumber + ipNumbers[1] + ipNumbers[2];
                }
            }
            return type;
        }

        private String getIPV6StringType(String ipv4String){
            String type = null;
            String[] ipNumberStrings = ipv4String.split(":");
            String[] ipNumberStringsReal = new String[]{"0000", "0000", "0000", "0000", "0000", "0000", "0000", "0000"};
            String[] suppleStrings = new String[]{"0000", "000", "00", "0", ""};
            int i = 0;
            while (i < ipNumberStrings.length){
                String ipNumberString = ipNumberStrings[i];
                if (ipNumberString.length() > 0){
                    ipNumberString = suppleStrings[ipNumberString.length()] + ipNumberString;
                    ipNumberStringsReal[i] = ipNumberString;
                } else {
                    break;
                }
                i++;
            }

            int j = ipNumberStrings.length - 1;
            int indexReal = ipNumberStringsReal.length - 1;
            while (i < j){
                String ipNumberString = ipNumberStrings[j];
                if (ipNumberString.length() > 0){
                    ipNumberString = suppleStrings[ipNumberString.length()] + ipNumberString;
                    ipNumberStringsReal[indexReal] = ipNumberString;
                } else {
                    break;
                }
                j--;
                indexReal--;
            }
            return type;
        }

    }

    private static class UploadIpGroup{
        private final String groupType;
        private final ArrayList<InetAddress> inetAddressArrayList;

        protected UploadIpGroup(String groupType,
                                ArrayList<InetAddress> inetAddressArrayList) {
            this.groupType = groupType;
            this.inetAddressArrayList = inetAddressArrayList;
        }

        protected InetAddress getInetAddress(){
            if (inetAddressArrayList == null || inetAddressArrayList.size() == 0){
                return null;
            } else {
                int index = (int)(Math.random()*inetAddressArrayList.size());
                return inetAddressArrayList.get(index);
            }
        }

    }

}
