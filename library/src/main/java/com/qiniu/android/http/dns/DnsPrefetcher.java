package com.qiniu.android.http.dns;

import com.qiniu.android.common.Config;
import com.qiniu.android.common.FixedZone;
import com.qiniu.android.common.Zone;
import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.common.ZonesInfo;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.storage.GlobalConfiguration;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.utils.AndroidNetwork;
import com.qiniu.android.utils.Utils;
import com.qiniu.android.utils.Wait;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by yangsen on 2020/5/28
 */
public class DnsPrefetcher {

    private boolean isPrefetching = false;
    private DnsCacheInfo dnsCacheInfo = null;
    private ConcurrentHashMap<String, List<IDnsNetworkAddress>> addressDictionary = new ConcurrentHashMap<>();
    private final HappyDns happyDns = new HappyDns();

    private final static DnsPrefetcher dnsPrefetcher = new DnsPrefetcher();
    private DnsPrefetcher(){
        happyDns.setQueryErrorHandler(new HappyDns.DnsQueryErrorHandler() {
            @Override
            public void queryError(Exception e, String host) {
                lastPrefetchErrorMessage = e.getMessage();
            }
        });
    }

    public static DnsPrefetcher getInstance(){
        return dnsPrefetcher;
    }

    public String lastPrefetchErrorMessage;

    public boolean recoverCache(){

        DnsCacheFile recorder = null;
        try {
            recorder = new DnsCacheFile(GlobalConfiguration.getInstance().dnsCacheDir);
        } catch (IOException e) {
            return true;
        }

        String localIp = AndroidNetwork.getHostIP();
        if (localIp == null || localIp.length() == 0){
            return true;
        }

        byte[] data = recorder.get(localIp);
        if (data == null){
            return true;
        }

        return recoverDnsCache(data);
    }

    public void localFetch(){
        if (!prepareToPreFetch()){
            return;
        }
        preFetchHosts(getLocalPreHost());
        recorderDnsCache();
        endPreFetch();
    }

    public boolean checkAndPrefetchDnsIfNeed(Zone currentZone, UpToken token){
        if (!prepareToPreFetch()){
            return false;
        }

        preFetchHosts(getCurrentZoneHosts(currentZone, token));
        recorderDnsCache();
        endPreFetch();
        return true;
    }

    public void invalidNetworkAddress(IDnsNetworkAddress address){
        if (address == null || address.getHostValue() == null){
            return;
        }
        String host = address.getHostValue();
        List<IDnsNetworkAddress> addressList = addressDictionary.get(host);
        ArrayList<IDnsNetworkAddress> addressListNew = new ArrayList<>();
        for (IDnsNetworkAddress addressP : addressList){
            if (!addressP.getIpValue().equals(addressP.getIpValue())){
                addressListNew.add(addressP);
            }
        }
        addressDictionary.put(host, addressListNew);
    }

    public List<IDnsNetworkAddress> getInetAddressByHost(String host){
        if (!isDnsOpen()){
            return null;
        }

        List<IDnsNetworkAddress> addressList = addressDictionary.get(host);
        if (addressList != null && addressList.size() > 0){
            return addressList;
        } else {
            return null;
        }
    }


    public void checkWhetherCachedDnsValid(){
        if (!prepareToPreFetch()){
            return;
        }

        preFetchHosts(addressDictionary.keySet().toArray(new String[0]));
        recorderDnsCache();
        endPreFetch();
    }


    private synchronized boolean prepareToPreFetch(){
        if (!isDnsOpen()){
            return false;
        }

        if (isPrefetching()){
            return false;
        }

        String localIp = AndroidNetwork.getHostIP();
        if (localIp == null || getDnsCacheInfo() == null || !(localIp.equals(getDnsCacheInfo().getLocalIp()))){
            clearPreHosts();
        }

        setPrefetching(true);
        return true;
    }

    private void endPreFetch(){
        setPrefetching(false);
    }

    private void preFetchHosts(String[] fetchHosts){
        String[] nextFetchHosts = fetchHosts;

        nextFetchHosts = preFetchHosts(nextFetchHosts, GlobalConfiguration.getInstance().dns);
        nextFetchHosts = preFetchHosts(nextFetchHosts, happyDns);
    }

    private String[] preFetchHosts(String[] preHosts, Dns dns){
        if (preHosts == null || preHosts.length == 0){
            return null;
        }
        if (dns == null){
            return preHosts;
        }

        ArrayList<String> failHosts = new ArrayList<>();
        for (String host : preHosts){
            int rePreNum = 0;
            boolean isSuccess = false;

            while (rePreNum < GlobalConfiguration.getInstance().dnsRepreHostNum){
                if (preFetchHost(host, dns)){
                    isSuccess = true;
                    break;
                }
                rePreNum += 1;
            }

            if (!isSuccess){
                failHosts.add(host);
            }
        }
        return failHosts.toArray(new String[0]);
     }

    private boolean preFetchHost(String preHost, Dns dns){
        if (preHost == null || preHost.length() == 0){
            return false;
        }

        List<IDnsNetworkAddress> preAddressList = addressDictionary.get(preHost);
        if (preAddressList != null && preAddressList.size() > 0){
            return true;
        }

        List<IDnsNetworkAddress> addressList = new ArrayList<>();
        try {
            List<IDnsNetworkAddress> preIAddressList = dns.lookup(preHost);
            if (preIAddressList != null && preIAddressList.size() > 0){
                for (IDnsNetworkAddress preIAddress : preIAddressList) {
                    DnsNetworkAddress address = new DnsNetworkAddress(preIAddress.getHostValue(),
                            preIAddress.getIpValue(),
                            preIAddress.getTtlValue() != null ? preIAddress.getTtlValue() : GlobalConfiguration.getInstance().dnsCacheTime,
                            preIAddress.getSourceValue(),
                            preIAddress.getTimestampValue());
                    addressList.add(address);
                }
            }
        } catch (UnknownHostException e) {}
        if (addressList != null && addressList.size() > 0){
            addressDictionary.put(preHost, addressList);
            return true;
        } else {
            return false;
        }
    }

    private boolean recoverDnsCache(byte[] data){

        DnsCacheInfo dnsCacheInfo = DnsCacheInfo.createDnsCacheInfoByData(data);
        if (dnsCacheInfo == null || dnsCacheInfo.info == null || dnsCacheInfo.info.size() == 0){
            return false;
        }

        addressDictionary.putAll(dnsCacheInfo.info);
        dnsCacheInfo.info = addressDictionary;
        setDnsCacheInfo(dnsCacheInfo);

        return false;
    }

    private boolean recorderDnsCache(){
        String currentTime = Utils.currentTimestamp() + "";
        String localIp = AndroidNetwork.getHostIP();

        if (localIp == null){
            return false;
        }

        DnsCacheInfo dnsCacheInfo = new DnsCacheInfo(currentTime, localIp, addressDictionary);

        DnsCacheFile recorder = null;
        try {
            recorder = new DnsCacheFile(GlobalConfiguration.getInstance().dnsCacheDir);
        } catch (IOException e) {
            return false;
        }

        setDnsCacheInfo(dnsCacheInfo);

        byte[] data = dnsCacheInfo.toJsonData();
        if (data == null){
            return false;
        }
        recorder.set(dnsCacheInfo.cacheKey(), data);

        return true;
    }

    private void clearPreHosts(){
        addressDictionary.clear();
    }


    private String[] getLocalPreHost(){
        ArrayList<String> localHosts = new ArrayList<>();

        String[] fixedHosts = getFixedZoneHosts();
        localHosts.addAll(Arrays.asList(fixedHosts));

        localHosts.add(Config.preQueryHost00);
        localHosts.add(Config.preQueryHost01);

        String logReport = Config.upLogURL;
        localHosts.add(logReport);

        return localHosts.toArray(new String[0]);
    }


//    private String[] getAllPreHost(Zone currentZone, UpToken token){
//
//        HashSet<String> fetchHosts = new HashSet<String>();
//
//        String[] fixedHosts = getFixedZoneHosts();
//        fetchHosts.addAll(Arrays.asList(fixedHosts));
//
//        String[] autoHosts = getCurrentZoneHosts(currentZone, token);
//        fetchHosts.addAll(Arrays.asList(autoHosts));
//
//        String[] cacheHosts = getCacheHosts();
//        fetchHosts.addAll(Arrays.asList(cacheHosts));
//
//        return fetchHosts.toArray(new String[0]);
//    }

    private String[] getCurrentZoneHosts(Zone currentZone, UpToken token){
        if (currentZone == null || token == null){
            return null;
        }

        final Wait wait = new Wait();

        currentZone.preQuery(token, new Zone.QueryHandler() {
            @Override
            public void complete(int code, ResponseInfo responseInfo, UploadRegionRequestMetrics metrics) {
                wait.stopWait();
            }
        });

        wait.startWait();

        ZonesInfo autoZonesInfo = currentZone.getZonesInfo(token);
        ArrayList<String> autoHosts = new ArrayList<>();
        if (autoZonesInfo != null && autoZonesInfo.zonesInfo != null && autoZonesInfo.zonesInfo.size() > 0) {
            for (ZoneInfo zoneInfo : autoZonesInfo.zonesInfo) {
                if (zoneInfo != null && zoneInfo.allHosts != null) {
                    autoHosts.addAll(zoneInfo.allHosts);
                }
            }
        }
        return autoHosts.toArray(new String[0]);
    }

    private String[] getFixedZoneHosts(){
        ArrayList<String> localHosts = new ArrayList<>();
        FixedZone fixedZone = FixedZone.localsZoneInfo();
        ZonesInfo zonesInfo = fixedZone.getZonesInfo(null);
        for (ZoneInfo zoneInfo : zonesInfo.zonesInfo) {
            if (zoneInfo != null && zoneInfo.allHosts != null){
                localHosts.addAll(zoneInfo.allHosts);
            }
        }
        return localHosts.toArray(new String[0]);
    }

    private String[] getCacheHosts(){
        return addressDictionary.keySet().toArray(new String[0]);
    }

    public boolean isDnsOpen(){
        return GlobalConfiguration.getInstance().isDnsOpen;
    }

    public synchronized boolean isPrefetching() {
        return isPrefetching;
    }
    private synchronized void setPrefetching(boolean isPrefetching) {
        this.isPrefetching = isPrefetching;
    }

    private synchronized DnsCacheInfo getDnsCacheInfo() {
        return dnsCacheInfo;
    }
    private synchronized void setDnsCacheInfo(DnsCacheInfo dnsCacheInfo) {
        this.dnsCacheInfo = dnsCacheInfo;
    }
}
