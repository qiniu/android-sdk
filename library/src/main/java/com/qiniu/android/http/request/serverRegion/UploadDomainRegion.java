package com.qiniu.android.http.request.serverRegion;

import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.http.request.UploadRegion;
import com.qiniu.android.http.request.UploadServerInterface;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class UploadDomainRegion implements UploadRegion {

    private boolean isAllFreezed;
    private HashMap<String, UploadServerDomain> domainHashMap;
    private HashMap<String, UploadServerDomain> oldDomainHashMap;
    private ZoneInfo zoneInfo;

    @Override
    public ZoneInfo getZoneInfo() {
        return zoneInfo;
    }

    @Override
    public void setupRegionData(@NotNull ZoneInfo zoneInfo) {
        if (zoneInfo == null) {
            return;
        }

        this.zoneInfo = zoneInfo;

        isAllFreezed = false;
        ArrayList<ZoneInfo.UploadServerGroup> serverGroups = new ArrayList<>();
        if (zoneInfo.acc != null){
            serverGroups.add(zoneInfo.acc);
        }
        if (zoneInfo.src != null){
            serverGroups.add(zoneInfo.src);
        }
        domainHashMap = createDomainDictionary(serverGroups);

        serverGroups = new ArrayList<>();
        if (zoneInfo.old_acc != null){
            serverGroups.add(zoneInfo.old_acc);
        }
        if (zoneInfo.old_src != null){
            serverGroups.add(zoneInfo.old_src);
        }
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
                domain.freeze();
            }
            domain = oldDomainHashMap.get(freezeServer.getServerId());
            if (domain != null){
                domain.freeze();
            }
        }

        HashMap<String, UploadServerDomain> domainInfo = isOldServer ? oldDomainHashMap : domainHashMap;
        UploadServerInterface server = null;
        for (String key : domainInfo.keySet()) {
            UploadServerDomain domain = domainInfo.get(key);
            if (domain != null){
                server =  domain.getNextServer();
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
        Date freezeDate = new Date(0);
        HashMap<String, UploadServerDomain> domainHashMap = new HashMap<>();
        for (int i = 0; i < serverGroups.size(); i++) {
            ZoneInfo.UploadServerGroup serverGroup = serverGroups.get(i);
            for (int j = 0; j < serverGroup.allHosts.size(); j++){
                String host = serverGroup.allHosts.get(j);
                UploadServerDomain domain = new UploadServerDomain(host,null, freezeDate);
                domainHashMap.put(host, domain);
            }
        }
        return  domainHashMap;
    }

    protected class UploadServerDomain{

        protected final String host;
        protected ArrayList<String> ipList;
        private Date freezeDate;

        UploadServerDomain(String host,
                           ArrayList<String> ipList,
                           Date freezeDate){
            this.host = host;
            this.ipList = ipList;
            this.freezeDate = freezeDate;
        }

        protected UploadServerInterface getNextServer(){
            if (host == null || host.length() == 0){
                return null;
            }

            Date currentDate = new Date();
            if (isFreezeByDate(currentDate)){
                return null;
            }
            String ip = null;
            if (ipList != null && ipList.size() > 0){
                ip = ipList.get(0);
            }
            return new UploadServer(host, host, ip);
        }

        boolean isFreezeByDate(Date date){
            boolean isFreeze = false;
            if (freezeDate.getTime() - date.getTime() > 0){
                isFreeze = true;
            }
            return isFreeze;
        }

        void freeze(){
            Date currentDate = new Date();
            freezeDate = new Date((currentDate.getTime() + 20*60*1000));
        }
    }
}
