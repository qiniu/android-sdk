package com.qiniu.android.http;

import com.qiniu.android.collect.Config;
import com.qiniu.android.common.Constants;
import com.qiniu.android.common.FixedZone;
import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.Recorder;
import com.qiniu.android.storage.persistent.DnsCacheFile;
import com.qiniu.android.utils.AndroidNetwork;
import com.qiniu.android.utils.StringUtils;
import com.qiniu.android.utils.UrlSafeBase64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

/**
 * <p>
 * Created by jemy on 2019/8/20.
 */

public class DnsPrefetcher {

    public static DnsPrefetcher dnsPrefetcher = null;
    private static String token;

    private static ConcurrentHashMap<String, List<InetAddress>> mConcurrentHashMap = new ConcurrentHashMap<String, List<InetAddress>>();
    private static List<String> mHosts = new ArrayList<String>();

    private DnsPrefetcher() {

    }

    public static DnsPrefetcher getDnsPrefetcher() {
        if (dnsPrefetcher == null) {
            synchronized (DnsPrefetcher.class) {
                if (dnsPrefetcher == null) {
                    dnsPrefetcher = new DnsPrefetcher();
                }
            }
        }
        return dnsPrefetcher;
    }

    public DnsPrefetcher init(String token) throws UnknownHostException {
        this.token = token;
        preHosts();
        preFetch();
        return this;
    }

    public void setConcurrentHashMap(ConcurrentHashMap<String, List<InetAddress>> mConcurrentHashMap) {
        this.mConcurrentHashMap = mConcurrentHashMap;
    }

    //use for test
    public List<String> getHosts() {
        return this.mHosts;
    }

    public void setHosts(List mHosts) {
        this.mHosts = mHosts;
    }

    //use for test
    public ConcurrentHashMap<String, List<InetAddress>> getConcurrentHashMap() {
        return this.mConcurrentHashMap;
    }

    //use for test
    public void setToken(String token) {
        this.token = token;
    }

    public List<InetAddress> getInetAddressByHost(String host) {
        return mConcurrentHashMap.get(host);
    }

    private void preHosts() {
        HashSet<String> set = new HashSet<String>();

        //preQuery sync
        ZoneInfo zoneInfo = getPreQueryZone();
        if (zoneInfo != null) {
            for (int i = 0; i < zoneInfo.upDomainsList.size(); i++) {
                if (set.add(zoneInfo.upDomainsList.get(i)))
                    mHosts.add(zoneInfo.upDomainsList.get(i));
            }
        }
        //local
        List<ZoneInfo> listZoneinfo = getLocalZone();
        for (ZoneInfo zone : listZoneinfo) {
            for (int i = 0; i < zone.upDomainsList.size(); i++) {
                if (set.add(zone.upDomainsList.get(i)))
                    mHosts.add(zone.upDomainsList.get(i));
            }
        }
        if (set.add(Config.preQueryHost))
            mHosts.add(Config.preQueryHost);
    }


    private void preFetch() {
        List<String> rePreHosts = new ArrayList<String>();
        for (int i = 0; i < mHosts.size(); i++) {
            List<InetAddress> inetAddresses = null;
            try {
                inetAddresses = okhttp3.Dns.SYSTEM.lookup(mHosts.get(i));
                mConcurrentHashMap.put(mHosts.get(i), inetAddresses);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                rePreHosts.add(mHosts.get(i));
            }
        }
        rePreFetch(rePreHosts, null);
    }

    /**
     * 对hosts预取失败对进行重新预取，deafult retryNum = 2
     *
     * @param rePreHosts 用于重试的hosts
     * @param customeDns 是否自定义dns
     */
    private void rePreFetch(List<String> rePreHosts, Dns customeDns) {
        for (int i = 0; i < rePreHosts.size(); i++) {
            int rePreNum = 0;
            while (rePreNum < Config.rePreHost) {
                rePreNum += 1;
                if (rePreFetch(rePreHosts.get(i), customeDns))
                    break;
            }
        }
    }

    private boolean rePreFetch(String host, Dns customeDns) {
        List<InetAddress> inetAddresses = null;
        try {
            if (customeDns == null) {
                inetAddresses = okhttp3.Dns.SYSTEM.lookup(host);
            } else {
                inetAddresses = customeDns.lookup(host);
            }
            mConcurrentHashMap.put(host, inetAddresses);
            return true;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 自定义dns预取
     *
     * @param dns
     * @return
     * @throws UnknownHostException
     */
    public void dnsPreByCustom(Dns dns) {
        List<String> rePreHosts = new ArrayList<String>();
        for (int i = 0; i < mHosts.size(); i++) {
            List<InetAddress> inetAddresses = null;
            try {
                inetAddresses = dns.lookup(mHosts.get(i));
                mConcurrentHashMap.put(mHosts.get(i), inetAddresses);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                rePreHosts.add(mHosts.get(i));
            }
        }
        rePreFetch(rePreHosts, dns);
    }

    /**
     * look local host
     */
    public List<ZoneInfo> getLocalZone() {
        List<ZoneInfo> listZoneInfo = FixedZone.getZoneInfos();
        return listZoneInfo;
    }


    /**
     * query host sync
     */
    public ZoneInfo getPreQueryZone() {
        DnsPrefetcher.ZoneIndex index = DnsPrefetcher.ZoneIndex.getFromToken(token);
        ZoneInfo zoneInfo = preQueryIndex(index);
        return zoneInfo;
    }

    ZoneInfo preQueryIndex(DnsPrefetcher.ZoneIndex index) {
        ZoneInfo info = null;
        try {
            ResponseInfo responseInfo = getZoneJsonSync(index);
            info = ZoneInfo.buildFromJson(responseInfo.response);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return info;
    }

    ResponseInfo getZoneJsonSync(DnsPrefetcher.ZoneIndex index) {
        Client client = new Client();
        String address = "http://" + Config.preQueryHost + "/v2/query?ak=" + index.accessKey + "&bucket=" + index.bucket;
        return client.syncGet(address, null);
    }


    static class ZoneIndex {
        final String accessKey;
        final String bucket;

        ZoneIndex(String accessKey, String bucket) {
            this.accessKey = accessKey;
            this.bucket = bucket;
        }

        static DnsPrefetcher.ZoneIndex getFromToken(String token) {
            String[] strings = token.split(":");
            String ak = strings[0];
            String policy = null;
            try {
                policy = new String(UrlSafeBase64.decode(strings[2]), Constants.UTF_8);
                JSONObject obj = new JSONObject(policy);
                String scope = obj.getString("scope");
                String bkt = scope.split(":")[0];
                return new DnsPrefetcher.ZoneIndex(ak, bkt);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        public int hashCode() {
            return accessKey.hashCode() * 37 + bucket.hashCode();
        }

        public boolean equals(Object obj) {
            return obj == this || !(obj == null || !(obj instanceof DnsPrefetcher.ZoneIndex))
                    && ((DnsPrefetcher.ZoneIndex) obj).accessKey.equals(accessKey) && ((DnsPrefetcher.ZoneIndex) obj).bucket.equals(bucket);
        }
    }

    /**
     * <p>
     * ip changed, the network has changed
     * ak:scope变化，prequery（v2）自动获取域名接口发生变化，存储区域可能变化
     * cacheTime>config.cacheTime（默认24H）
     * </p>
     *
     * @return true:重新预期并缓存, false:不需要重新预取和缓存
     */
    public static boolean checkRePrefetchDns(String token, Configuration config) {
        Recorder recorder = null;
        try {
            recorder = new DnsCacheFile(Config.dnscacheDir);
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }
        String dnscache = recorder.getFileName();
        if (dnscache == null)
            return true;

        byte[] data = recorder.get(dnscache);
        if (data == null)
            return true;

        String[] cacheKey = dnscache.split(":");
        if (cacheKey.length < 3)
            return true;

        String currentTime = String.valueOf(System.currentTimeMillis());
        String localip = AndroidNetwork.getHostIP();
        String akAndScope = StringUtils.getAkAndScope(token);

        long cacheTime = (Long.parseLong(currentTime) - Long.parseLong(cacheKey[0])) / 1000;
        if (!localip.equals(cacheKey[1]) || cacheTime > config.dnsCacheTimeMs || !akAndScope.equals(cacheKey[2])) {
            return true;
        }

        return recoverDnsCache(data);
    }

    /**
     * start preFetchDns: Time-consuming operation, in a thread
     *
     * @param token
     */
    public static void startPrefetchDns(String token, Configuration config) {
        String currentTime = String.valueOf(System.currentTimeMillis());
        String localip = AndroidNetwork.getHostIP();
        String akAndScope = StringUtils.getAkAndScope(token);
        String cacheKey = format(Locale.ENGLISH, "\"time:\":%s\"ip:\":%s\"ak\":%s", currentTime, localip, akAndScope);
        Recorder recorder = null;
        DnsPrefetcher dnsPrefetcher = null;
        try {
            recorder = new DnsCacheFile(Config.dnscacheDir);
            dnsPrefetcher = DnsPrefetcher.getDnsPrefetcher().init(token);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (config.dns != null) {
            DnsPrefetcher.getDnsPrefetcher().dnsPreByCustom(config.dns);
        }
        ConcurrentHashMap<String, List<InetAddress>> concurrentHashMap = dnsPrefetcher.getConcurrentHashMap();
        byte[] dnscache = StringUtils.toByteArray(concurrentHashMap);

        recorder.set(cacheKey, dnscache);
    }

    /**
     * @param data
     * @return
     */
    public static boolean recoverDnsCache(byte[] data) {
        ConcurrentHashMap<String, List<InetAddress>> concurrentHashMap = (ConcurrentHashMap<String, List<InetAddress>>) StringUtils.toObject(data);
        if (concurrentHashMap == null) {
            return true;
        }
        DnsPrefetcher.getDnsPrefetcher().setConcurrentHashMap(concurrentHashMap);

        ArrayList<String> list = new ArrayList<String>();
        Iterator iter = concurrentHashMap.keySet().iterator();
        while (iter.hasNext()) {
            String tmpkey = (String) iter.next();
            if (tmpkey == null || tmpkey.length() == 0)
                continue;
            list.add(tmpkey);
        }
        DnsPrefetcher.getDnsPrefetcher().setHosts(list);
        return false;
    }
}
