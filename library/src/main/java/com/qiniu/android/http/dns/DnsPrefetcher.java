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
import com.qiniu.android.storage.persistent.DnsCacheFile;
import com.qiniu.android.utils.AndroidNetwork;
import com.qiniu.android.utils.UrlSafeBase64;
import com.qiniu.android.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by yangsen on 2020/5/28
 */
public class DnsPrefetcher {

    private boolean isPrefetching = false;
    private DnsCacheKey dnsCacheKey = null;
    private HashMap<String, List<InetAddress>> addressDictionary = new HashMap<>();
    private final SystemDns systemDns = new SystemDns();

    private final static DnsPrefetcher dnsPrefetcher = new DnsPrefetcher();
    private DnsPrefetcher(){}

    public static DnsPrefetcher getInstance(){
        return dnsPrefetcher;
    }

    public boolean recoverCache(){

        DnsCacheFile recoder = null;
        try {
            recoder = new DnsCacheFile(Config.dnscacheDir);
        } catch (IOException e) {
            return true;
        }

        String dnsCache = recoder.getFileName();
        if (dnsCache == null || dnsCache.length() == 0){
            return true;
        }

        byte[] data = recoder.get(dnsCache);
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
        recoderDnsCache();
        endPreFetch();
    }

    public boolean checkAndPrefetchDnsIfNeed(Zone currentZone, String token){
        if (!prepareToPreFetch()){
            return false;
        }

        preFetchHosts(getCurrentZoneHosts(currentZone, token));
        recoderDnsCache();
        endPreFetch();
        return true;
    }

    public void invalidInetAdress(InetAddress inetAddress){
        List<InetAddress> inetAddressList = addressDictionary.get(inetAddress.getHostAddress());
        ArrayList<InetAddress> inetAddressListNew = new ArrayList<>();
        for (InetAddress inetAddressP : inetAddressList){
            if (!inetAddressP.equals(inetAddress)){
                inetAddressListNew.add(inetAddressP);
            }
        }
        addressDictionary.put(inetAddress.getHostName(), inetAddressListNew);
    }

    public List<InetAddress> getInetAddressByHost(String host){
        if (!isDnsOpen()){
            return null;
        }

        List<InetAddress> addressList = addressDictionary.get(host);
        if (addressList != null && addressList.size() > 0){
            return addressList;
        } else {
            return null;
        }
    }


    private void checkWhetherCachedDnsValid(){
        if (!prepareToPreFetch()){
            return;
        }

        preFetchHosts(addressDictionary.keySet().toArray(new String[0]));
        recoderDnsCache();
        endPreFetch();
    }


    private boolean prepareToPreFetch(){
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

        setIsPrefetching(true);
        return true;
    }

    private void endPreFetch(){
        setIsPrefetching(false);
    }

    private void preFetchHosts(String[] fetchHosts){
        String[] nextFetchHosts = fetchHosts;

        nextFetchHosts = preFetchHosts(nextFetchHosts, Config.dns);
        nextFetchHosts = preFetchHosts(nextFetchHosts, systemDns);
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

        List<InetAddress> preAddressList = addressDictionary.get(preHost);
        if (preAddressList != null && preAddressList.size() > 0){
            return true;
        }

        List<InetAddress> addressList = null;
        try {
            addressList = dns.lookup(preHost);
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
            ArrayList<InetAddress> addressList = new ArrayList<>();
            try {
                JSONArray addressDicList = addressInfo.getJSONArray(host);
                for(int i=0; i<addressDicList.length(); i++){
                    JSONObject addressDic = addressDicList.getJSONObject(i);
                    String hostP = addressDic.getString("host");
                    String ipString = addressDic.getString("ip");
                    InetAddress inetAddress = InetAddress.getByAddress(hostP, UrlSafeBase64.decode(ipString));
                    addressList.add(inetAddress);
                }
            } catch (JSONException ignored) {
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            addressDictionary.put(host, addressList);
        }

        return false;
    }

    private boolean recoderDnsCache(){
        String currentTime = Utils.currentTimestamp() + "";
        String localIp = AndroidNetwork.getHostIP();

        if (localIp == null){
            return false;
        }

        DnsCacheKey dnsCacheKey = new DnsCacheKey(currentTime, localIp);

        String cacheKey = dnsCacheKey.toString();

        DnsCacheFile recoder = null;
        try {
            recoder = new DnsCacheFile(Config.dnscacheDir);
        } catch (IOException e) {
            return false;
        }

        setDnsCacheKey(dnsCacheKey);

        return recoderDnsCache(recoder, cacheKey);
    }

    private boolean recoderDnsCache(Recorder recorder, String cacheKey){

        JSONObject addressInfo = new JSONObject();
        for (String key : addressDictionary.keySet()){
            List<InetAddress> addressModelList = addressDictionary.get(key);
            JSONArray addressDicList = new JSONArray();

            for (InetAddress ipInfo : addressModelList){
                JSONObject addressDic = new JSONObject();
                if (ipInfo.getHostName() != null && ipInfo.getHostAddress() != null) {
                    try {
                        addressDic.put("host", ipInfo.getHostName());
                        addressDic.put("ip", UrlSafeBase64.encodeToString(ipInfo.getAddress()));
                        addressDicList.put(addressDic);
                    } catch (JSONException ignored) {
                    }
                }
            }

            try {
                addressInfo.put(key, addressDicList);
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


    private String[] getAllPreHost(Zone currentZone, String token){

        HashSet<String> fetchHosts = new HashSet<String>();

        String[] fixedHosts = getFixedZoneHosts();
        fetchHosts.addAll(Arrays.asList(fixedHosts));

        String[] autoHosts = getCurrentZoneHosts(currentZone, token);
        fetchHosts.addAll(Arrays.asList(autoHosts));

        String[] cacheHosts = getCacheHosts();
        fetchHosts.addAll(Arrays.asList(cacheHosts));

        return fetchHosts.toArray(new String[0]);
    }

    private String[] getCurrentZoneHosts(Zone currentZone, String token){
        if (currentZone == null || token == null){
            return null;
        }

        final CountDownLatch completeSignal = new CountDownLatch(1);
        currentZone.preQuery(UpToken.parse(token), new Zone.QueryHandler() {
            @Override
            public void complete(int code, ResponseInfo responseInfo, UploadRegionRequestMetrics metrics) {
                completeSignal.countDown();
            }
        });
        try {
            completeSignal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {}

        ZonesInfo autoZonesInfo = currentZone.getZonesInfo(UpToken.parse(token));
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

    private boolean isDnsOpen(){
        return true;
    }

    public synchronized boolean isPrefetching() {
        return isPrefetching;
    }
    public synchronized void setIsPrefetching(boolean isPrefetching) {
        this.isPrefetching = isPrefetching;
    }

    public synchronized DnsCacheKey getDnsCacheKey() {
        return dnsCacheKey;
    }
    public synchronized void setDnsCacheKey(DnsCacheKey dnsCacheKey) {
        this.dnsCacheKey = dnsCacheKey;
    }
}
