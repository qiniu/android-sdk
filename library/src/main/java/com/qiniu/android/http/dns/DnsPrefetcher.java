package com.qiniu.android.http.dns;

import com.qiniu.android.common.Config;
import com.qiniu.android.common.FixedZone;
import com.qiniu.android.common.Zone;
import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.common.ZonesInfo;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.storage.Recorder;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.utils.AndroidNetwork;
import com.qiniu.android.utils.Utils;
import com.qiniu.android.utils.Wait;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Created by yangsen on 2020/5/28
 */
public class DnsPrefetcher {

    private boolean isPrefetching = false;
    private DnsCacheKey dnsCacheKey = null;
    private HashMap<String, List<IDnsNetworkAddress>> addressDictionary = new HashMap<>();
    private final HappyDns happyDns = new HappyDns();

    private final static DnsPrefetcher dnsPrefetcher = new DnsPrefetcher();
    private DnsPrefetcher(){
        happyDns.setQueryErrorHandler(new HappyDns.DnsQueryErrorHandler() {
            @Override
            public void queryError(Exception e, String host) {
                lastPrefetchedErrorMessage = e.getMessage();
            }
        });
    }

    public static DnsPrefetcher getInstance(){
        return dnsPrefetcher;
    }

    public String lastPrefetchedErrorMessage;

    public boolean recoverCache(){

        DnsCacheFile recorder = null;
        try {
            recorder = new DnsCacheFile(Config.dnscacheDir);
        } catch (IOException e) {
            return true;
        }

        String dnsCache = recorder.getFileName();
        if (dnsCache == null || dnsCache.length() == 0){
            return true;
        }

        byte[] data = recorder.get(dnsCache);
        if (data == null){
            return true;
        }

        DnsCacheKey cacheKey = DnsCacheKey.toCacheKey(dnsCache);
        if (cacheKey == null){
            return true;
        }

        String localIp = AndroidNetwork.getHostIP();
        if (localIp == null || localIp.length() == 0 || !localIp.equals(cacheKey.getLocalIp())){
            return true;
        }

        setDnsCacheKey(cacheKey);

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
        if (localIp == null || getDnsCacheKey() == null || !(localIp.equals(getDnsCacheKey().getLocalIp()))){
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

        nextFetchHosts = preFetchHosts(nextFetchHosts, Config.dns);
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

            while (rePreNum < Config.dnsRepreHostNum){
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
                            preIAddress.getTtlValue(),
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

        JSONObject addressInfo = null;
        try {
            addressInfo = new JSONObject(new String(data));
        } catch (JSONException e) {
            return false;
        }

        Iterator<String> hosts = addressInfo.keys();
        while (hosts.hasNext()){
            String host = hosts.next();
            ArrayList<IDnsNetworkAddress> addressList = new ArrayList<>();
            try {
                JSONArray addressJsonList = addressInfo.getJSONArray(host);
                for(int i=0; i<addressJsonList.length(); i++){
                    JSONObject addressJson = addressJsonList.getJSONObject(i);
                    DnsNetworkAddress address = DnsNetworkAddress.address(addressJson);
                    addressList.add(address);
                }
            } catch (JSONException ignored) {
            }

            addressDictionary.put(host, addressList);
        }

        return false;
    }

    private boolean recorderDnsCache(){
        String currentTime = Utils.currentTimestamp() + "";
        String localIp = AndroidNetwork.getHostIP();

        if (localIp == null){
            return false;
        }

        DnsCacheKey dnsCacheKey = new DnsCacheKey(currentTime, localIp);

        String cacheKey = dnsCacheKey.toString();

        DnsCacheFile recorder = null;
        try {
            recorder = new DnsCacheFile(Config.dnscacheDir);
        } catch (IOException e) {
            return false;
        }

        setDnsCacheKey(dnsCacheKey);

        return recorderDnsCache(recorder, cacheKey);
    }

    private boolean recorderDnsCache(Recorder recorder, String cacheKey){

        JSONObject addressInfo = new JSONObject();
        for (String key : addressDictionary.keySet()){
            List<IDnsNetworkAddress> addressModelList = addressDictionary.get(key);
            JSONArray addressJsonList = new JSONArray();

            for (IDnsNetworkAddress address : addressModelList){
                if (address.getHostValue() != null && address.getIpValue() != null) {
                    DnsNetworkAddress addressObject = (DnsNetworkAddress)address;
                    JSONObject addressJson = addressObject.toJson();
                    addressJsonList.put(addressJson);
                }
            }

            try {
                addressInfo.put(key, addressJsonList);
            } catch (JSONException ignored) {}
        }

        recorder.set(cacheKey, addressInfo.toString().getBytes());
        return true;
    }

    private void clearPreHosts(){
        addressDictionary.clear();
    }


    private String[] getLocalPreHost(){
        ArrayList<String> localHosts = new ArrayList<>();

        String[] fixedHosts = getFixedZoneHosts();
        localHosts.addAll(Arrays.asList(fixedHosts));

        String ucHost = Config.preQueryHost;
        localHosts.add(ucHost);

        String logReport = Config.upLogURL;
        localHosts.add(logReport);

        return localHosts.toArray(new String[0]);
    }


    private String[] getAllPreHost(Zone currentZone, UpToken token){

        HashSet<String> fetchHosts = new HashSet<String>();

        String[] fixedHosts = getFixedZoneHosts();
        fetchHosts.addAll(Arrays.asList(fixedHosts));

        String[] autoHosts = getCurrentZoneHosts(currentZone, token);
        fetchHosts.addAll(Arrays.asList(autoHosts));

        String[] cacheHosts = getCacheHosts();
        fetchHosts.addAll(Arrays.asList(cacheHosts));

        return fetchHosts.toArray(new String[0]);
    }

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
        for (ZoneInfo zoneInfo : autoZonesInfo.zonesInfo) {
            if (zoneInfo != null && zoneInfo.allHosts != null){
                autoHosts.addAll(zoneInfo.allHosts);
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
        return true;
    }

    public synchronized boolean isPrefetching() {
        return isPrefetching;
    }
    private synchronized void setPrefetching(boolean isPrefetching) {
        this.isPrefetching = isPrefetching;
    }

    private synchronized DnsCacheKey getDnsCacheKey() {
        return dnsCacheKey;
    }
    private synchronized void setDnsCacheKey(DnsCacheKey dnsCacheKey) {
        this.dnsCacheKey = dnsCacheKey;
    }
}
