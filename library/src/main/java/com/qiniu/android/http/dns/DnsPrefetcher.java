package com.qiniu.android.http.dns;

import com.qiniu.android.common.Config;
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
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by yangsen on 2020/5/28
 */
public class DnsPrefetcher {

    private Dns customDns;
    private SystemDns systemDns;

    private boolean isPrefetching = false;
    private DnsCacheInfo dnsCacheInfo = null;
    private HashSet<String> prefetchHosts = new HashSet<>();
    private ConcurrentHashMap<String, List<IDnsNetworkAddress>> addressDictionary = new ConcurrentHashMap<>();
    private DnsCacheFile diskCache;

    private final static DnsPrefetcher dnsPrefetcher = new DnsPrefetcher();

    private DnsPrefetcher() {
        systemDns = new SystemDns(GlobalConfiguration.getInstance().dnsResolveTimeout);
    }

    public static DnsPrefetcher getInstance() {
        return dnsPrefetcher;
    }

    public String lastPrefetchErrorMessage;

    public boolean recoverCache() {

        DnsCacheFile recorder = getDiskCache();
        if (recorder == null) {
            return false;
        }

        String localIp = AndroidNetwork.getHostIP();
        if (localIp == null || localIp.length() == 0) {
            return true;
        }

        byte[] data = recorder.get(localIp);
        if (data == null) {
            return true;
        }

        return recoverDnsCache(data);
    }

    public void localFetch() {
        addPreFetchHosts(getLocalPreHost());
    }

    public boolean checkAndPrefetchDnsIfNeed(Zone currentZone, UpToken token) {
        return addPreFetchHosts(getCurrentZoneHosts(currentZone, token));
    }

    public boolean addPreFetchHosts(String[] hosts) {
        if (hosts == null) {
            return false;
        }

        // 已经添加则不再触发预取
        boolean prefetchHostsContainHosts = true;
        synchronized (this) {
            int countBeforeAdd = prefetchHosts.size();
            prefetchHosts.addAll(Arrays.asList(hosts));
            int countAfterAdd = prefetchHosts.size();
            if (countAfterAdd > countBeforeAdd) {
                prefetchHostsContainHosts = false;
            }
        }

        if (!prefetchHostsContainHosts) {
            checkWhetherCachedDnsValid();
            return true;
        } else {
            return false;
        }
    }

    public void invalidNetworkAddress(IDnsNetworkAddress address) {
        if (address == null || address.getHostValue() == null) {
            return;
        }

        String host = address.getHostValue();
        List<IDnsNetworkAddress> addressList = addressDictionary.get(host);
        if (addressList == null || addressList.size() == 0) {
            return;
        }

        ArrayList<IDnsNetworkAddress> addressListNew = new ArrayList<>();
        for (IDnsNetworkAddress addressP : addressList) {
            if (!addressP.getIpValue().equals(addressP.getIpValue())) {
                addressListNew.add(addressP);
            }
        }
        addressDictionary.put(host, addressListNew);
    }

    private void invalidNetworkAddressOfHost(String host) {
        if (host == null || host.length() == 0) {
            return;
        }
        addressDictionary.remove(host);
    }

    public List<IDnsNetworkAddress> getInetAddressByHost(String host) {
        if (!isDnsOpen()) {
            return null;
        }

        List<IDnsNetworkAddress> addressList = addressDictionary.get(host);
        if (addressList != null && addressList.size() > 0) {
            DnsNetworkAddress firstAddress = (DnsNetworkAddress) addressList.get(0);
            if (firstAddress.isValid()) {
                return addressList;
            }
        }

        return null;
    }

    public String lookupBySafeDns(String hostname) throws UnknownHostException {
        if (hostname == null || hostname.length() == 0) {
            return null;
        }

        invalidNetworkAddressOfHost(hostname);

        String[] nextFetchHosts = new String[]{hostname};
        int dnsTimeout = GlobalConfiguration.getInstance().dnsResolveTimeout;

        // 自定义 dns
        nextFetchHosts = preFetchHosts(nextFetchHosts, getCustomDns());
        if (nextFetchHosts == null || nextFetchHosts.length == 0) {
            List<IDnsNetworkAddress> addresses = getInetAddressByHost(hostname);
            if (addresses != null && addresses.size() > 0) {
                return addresses.get(0).getSourceValue();
            }
        }

        // http dns
        HttpDns httpDns = new HttpDns(dnsTimeout);
        nextFetchHosts = preFetchHosts(nextFetchHosts, httpDns);
        if (nextFetchHosts == null || nextFetchHosts.length == 0) {
            List<IDnsNetworkAddress> addresses = getInetAddressByHost(hostname);
            if (addresses != null && addresses.size() > 0) {
                return addresses.get(0).getSourceValue();
            }
        }

        return null;
    }

    public void clearDnsCache() throws IOException {
        clearMemoryCache();
        clearDiskCache();
    }

    public void checkWhetherCachedDnsValid() {
        if (!prepareToPreFetch()) {
            return;
        }

        String[] hosts = null;
        synchronized (this) {
            hosts = getCacheHosts();
        }
        preFetchHosts(hosts);
        endPreFetch();
    }


    private synchronized boolean prepareToPreFetch() {
        if (!isDnsOpen()) {
            return false;
        }

        if (isPrefetching()) {
            return false;
        }

        String localIp = AndroidNetwork.getHostIP();
        if (localIp == null || getDnsCacheInfo() == null || !(localIp.equals(getDnsCacheInfo().getLocalIp()))) {
            clearMemoryCache();
        }

        setPrefetching(true);
        return true;
    }

    private void endPreFetch() {
        setPrefetching(false);
    }

    private void preFetchHosts(String[] fetchHosts) {

        String[] nextFetchHosts = fetchHosts;
        int dnsTimeout = GlobalConfiguration.getInstance().dnsResolveTimeout;

        // 自定义 dns
        nextFetchHosts = preFetchHosts(nextFetchHosts, getCustomDns());
        if (nextFetchHosts == null || nextFetchHosts.length == 0) {
            return;
        }

        // 系统 dns
        nextFetchHosts = preFetchHosts(nextFetchHosts, systemDns);
        if (nextFetchHosts == null || nextFetchHosts.length == 0) {
            return;
        }

        // http dns
        HttpDns httpDns = new HttpDns(dnsTimeout);
        nextFetchHosts = preFetchHosts(nextFetchHosts, httpDns);
        if (nextFetchHosts == null || nextFetchHosts.length == 0) {
            return;
        }

        // udp dns
        UdpDns udpDns = new UdpDns(dnsTimeout);
        nextFetchHosts = preFetchHosts(nextFetchHosts, udpDns);

        recorderDnsCache();
    }

    private String[] preFetchHosts(String[] preHosts, Dns dns) {
        if (preHosts == null || preHosts.length == 0) {
            return null;
        }
        if (dns == null) {
            return preHosts;
        }

        UnknownHostException exception = null;
        ArrayList<String> failHosts = new ArrayList<>();
        for (String host : preHosts) {
            int rePreNum = 0;
            boolean isSuccess = false;

            while (rePreNum < GlobalConfiguration.getInstance().dnsRepreHostNum) {
                try {
                    isSuccess = preFetchHost(host, dns);
                } catch (UnknownHostException e) {
                    lastPrefetchErrorMessage = e.toString();
                }
                if (isSuccess) {
                    break;
                }
                rePreNum += 1;
            }

            if (!isSuccess) {
                failHosts.add(host);
            }
        }
        return failHosts.toArray(new String[0]);
    }

    private boolean preFetchHost(String preHost, Dns dns) throws UnknownHostException {
        if (preHost == null || preHost.length() == 0) {
            return false;
        }

        List<IDnsNetworkAddress> preAddressList = addressDictionary.get(preHost);
        if (preAddressList != null && preAddressList.size() > 0) {
            DnsNetworkAddress firstAddress = (DnsNetworkAddress) preAddressList.get(0);
            if (!firstAddress.needRefresh()) {
                return true;
            }
        }

        boolean isCustomDns = (dns == getCustomDns());
        UnknownHostException exception = null;
        List<IDnsNetworkAddress> addressList = new ArrayList<>();
        try {
            List<IDnsNetworkAddress> preIAddressList = dns.lookup(preHost);
            if (preIAddressList != null && preIAddressList.size() > 0) {
                for (IDnsNetworkAddress preIAddress : preIAddressList) {
                    DnsNetworkAddress address = new DnsNetworkAddress(preIAddress.getHostValue(),
                            preIAddress.getIpValue(),
                            preIAddress.getTtlValue() != null ? preIAddress.getTtlValue() : GlobalConfiguration.getInstance().dnsCacheTime,
                            isCustomDns ? DnsSource.Custom : preIAddress.getSourceValue(),
                            preIAddress.getTimestampValue());
                    addressList.add(address);
                }
            }
        } catch (UnknownHostException e) {
            exception = e;
        }

        if (addressList.size() > 0) {
            addressDictionary.put(preHost, addressList);
            return true;
        } else if (exception != null) {
            throw exception;
        } else {
            return false;
        }
    }

    private boolean recoverDnsCache(byte[] data) {

        DnsCacheInfo dnsCacheInfo = DnsCacheInfo.createDnsCacheInfoByData(data);
        if (dnsCacheInfo == null || dnsCacheInfo.getInfo() == null || dnsCacheInfo.getInfo().size() == 0) {
            return false;
        }

        addressDictionary.putAll(dnsCacheInfo.getInfo());
        dnsCacheInfo.setInfo(addressDictionary);
        setDnsCacheInfo(dnsCacheInfo);

        return false;
    }

    private boolean recorderDnsCache() {
        DnsCacheFile recorder = getDiskCache();
        if (recorder == null) {
            return false;
        }

        String currentTime = Utils.currentTimestamp() + "";
        String localIp = AndroidNetwork.getHostIP();
        if (localIp == null) {
            return false;
        }

        DnsCacheInfo dnsCacheInfo = new DnsCacheInfo(currentTime, localIp, addressDictionary);
        setDnsCacheInfo(dnsCacheInfo);

        byte[] data = dnsCacheInfo.toJsonData();
        if (data == null) {
            return false;
        }
        recorder.set(dnsCacheInfo.cacheKey(), data);

        return true;
    }

    public void clearMemoryCache() {
        addressDictionary.clear();
    }

    public void clearDiskCache() throws IOException {
        DnsCacheFile recorder = getDiskCache();
        if (recorder == null) {
            return;
        }
        recorder.clearCache();
    }


    private String[] getLocalPreHost() {
        return new String[]{Config.upLogURL};
    }

    private String[] getCurrentZoneHosts(Zone currentZone, UpToken token) {
        if (currentZone == null || token == null) {
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

    private String[] getCacheHosts() {
        return prefetchHosts.toArray(new String[0]);
    }

    public boolean isDnsOpen() {
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

    private synchronized DnsCacheFile getDiskCache() {
        if (diskCache == null) {
            try {
                diskCache = new DnsCacheFile(GlobalConfiguration.getInstance().dnsCacheDir);
            } catch (Exception ignored) {
                diskCache = null;
            }
        }
        return diskCache;
    }

    private synchronized Dns getCustomDns() {
        if (customDns == null) {
            customDns = GlobalConfiguration.getInstance().dns;
        }
        return customDns;
    }

    private synchronized void setDnsCacheInfo(DnsCacheInfo dnsCacheInfo) {
        this.dnsCacheInfo = dnsCacheInfo;
    }
}
