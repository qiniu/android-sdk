package com.qiniu.android.http;

import com.qiniu.android.collect.Config;
import com.qiniu.android.collect.LogHandler;
import com.qiniu.android.collect.UploadInfo;
import com.qiniu.android.collect.UploadInfoElementCollector;
import com.qiniu.android.common.Constants;
import com.qiniu.android.common.FixedZone;
import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.http.custom.DnsCacheKey;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>
 * Created by jemy on 2019/8/20.
 */

public class DnsPrefetcher {

    public static DnsPrefetcher dnsPrefetcher = null;
    private static String token;
    private static Configuration config;

    private static ConcurrentHashMap<String, List<InetAddress>> mConcurrentHashMap = new ConcurrentHashMap<String, List<InetAddress>>();
    private static AtomicReference mDnsCacheKey = new AtomicReference();

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

    /**
     * 不用判断是否预取，每次调用UploadManager都直接预取一次。
     * uploadManager只会new一次，但是uploadManager.put方法有多次，如果期间网络发生变化
     * 这个方法预取的结果应该被ConcurrentHashMap自动覆盖
     */
    public void localFetch() {
        List<String> localHosts = new ArrayList<String>();
        //local
        List<ZoneInfo> listZoneinfo = getLocalZone();
        for (ZoneInfo zone : listZoneinfo) {
            for (String host : zone.upDomainsList) {
                localHosts.add(host);
            }
        }
        localHosts.add(Config.preQueryHost);

        if (localHosts != null && localHosts.size() > 0)
            preFetch(localHosts);
    }

    public DnsPrefetcher init(String token, Configuration config) throws UnknownHostException {
        this.token = token;
        this.config = config;
        List<String> preHosts = preHosts();
        if (preHosts != null && preHosts.size() > 0)
            preFetch(preHosts);
        return this;
    }

    public void setConcurrentHashMap(ConcurrentHashMap<String, List<InetAddress>> mConcurrentHashMap) {
        this.mConcurrentHashMap = mConcurrentHashMap;
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

    private List<String> preHosts() {
        HashSet<String> set = new HashSet<String>();
        List<String> preHosts = new ArrayList<>();
        //preQuery sync
        ZoneInfo zoneInfo = getPreQueryZone();
        if (zoneInfo != null) {
            for (String host : zoneInfo.upDomainsList) {
                if (set.add(host))
                    preHosts.add(host);
            }
        }

        //local
        List<ZoneInfo> listZoneinfo = getLocalZone();
        for (ZoneInfo zone : listZoneinfo) {
            for (String host : zone.upDomainsList) {
                if (set.add(host))
                    preHosts.add(host);
            }
        }
        if (set.add(Config.preQueryHost))
            preHosts.add(Config.preQueryHost);
        return preHosts;
    }


    private void preFetch(List<String> fetchHost) {
        List<String> rePreHosts = new ArrayList<String>();
        for (String host : fetchHost) {
            List<InetAddress> inetAddresses = null;
            try {
                inetAddresses = okhttp3.Dns.SYSTEM.lookup(host);
                mConcurrentHashMap.put(host, inetAddresses);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                rePreHosts.add(host);
            }
        }
        if (rePreHosts.size() > 0)
            rePreFetch(rePreHosts, null);
    }

    /**
     * 对hosts预取失败对进行重新预取，deafult retryNum = 2
     *
     * @param rePreHosts 用于重试的hosts
     * @param customeDns 是否自定义dns
     */
    private void rePreFetch(List<String> rePreHosts, Dns customeDns) {
        for (String host : rePreHosts) {
            int rePreNum = 0;
            while (rePreNum < Config.rePreHost) {
                rePreNum += 1;
                if (rePreFetch(host, customeDns))
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
        if (mConcurrentHashMap != null && mConcurrentHashMap.size() > 0) {
            Iterator iter = mConcurrentHashMap.keySet().iterator();
            while (iter.hasNext()) {
                String tmpkey = (String) iter.next();
                if (!(tmpkey == null) && !(tmpkey.length() == 0)) {
                    List<InetAddress> inetAddresses = null;
                    try {
                        inetAddresses = dns.lookup(tmpkey);
                        mConcurrentHashMap.put(tmpkey, inetAddresses);
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                        rePreHosts.add(tmpkey);
                    }
                }
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

    public static String target_region_id = "";

    ZoneInfo preQueryIndex(DnsPrefetcher.ZoneIndex index) {
        ZoneInfo info = null;
        try {
            ResponseInfo responseInfo = getZoneJsonSync(index);
            if (responseInfo.response == null)
                return null;
            info = ZoneInfo.buildFromJson(responseInfo.response);
            if (info == null) return null;
            if (info.upDomainsList.size() > 0) {
                if (info.upDomainsList.contains(FixedZone.arrayzone0[0])) {
                    target_region_id = "z0";
                } else if (info.upDomainsList.contains(FixedZone.arrayzone1[0])) {
                    target_region_id = "z1";
                } else if (info.upDomainsList.contains(FixedZone.arrayzone1[0])) {
                    target_region_id = "z2";
                } else if (info.upDomainsList.contains(FixedZone.arrayZoneAs0[0])) {
                    target_region_id = "as0";
                } else if (info.upDomainsList.contains(FixedZone.arrayzoneNa0[0])) {
                    target_region_id = "na";
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return info;
    }

    ResponseInfo getZoneJsonSync(DnsPrefetcher.ZoneIndex index) {
        Client client = new Client();
        String schema = "https://";
        if (!config.useHttps) {
            schema = "http://";
        }
        String address = schema + Config.preQueryHost + "/v2/query?ak=" + index.accessKey + "&bucket=" + index.bucket;

        LogHandler logHandler = UploadInfoElementCollector.getUplogHandler(UploadInfo.getReqInfo());
        logHandler.send("up_type", "uc_query");

        return client.syncGet(logHandler, address, null);
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
        if (mDnsCacheKey.get() == null)
            return true;

        String currentTime = String.valueOf(System.currentTimeMillis());
        String localip = AndroidNetwork.getHostIP();
        String akScope = StringUtils.getAkAndScope(token);

        if (currentTime == null || localip == null || akScope == null)
            return true;
        DnsCacheKey dnsCacheKey = (DnsCacheKey) mDnsCacheKey.get();
        if (dnsCacheKey == null || dnsCacheKey.getCurrentTime() == null)
            return true;
        long cacheTime = (Long.parseLong(currentTime) - Long.parseLong(dnsCacheKey.getCurrentTime())) / 1000;
        if (!localip.equals(dnsCacheKey.getLocalIp()) || cacheTime > config.dnsCacheTimeMs || !akScope.equals(dnsCacheKey.getAkScope())) {
            return true;
        }

        return false;
    }

    /**
     * uploadManager初始化时，加载本地缓存到内存
     *
     * @param config
     * @return 如果不存在缓存返回true，需要重新预取；如果存在且满足使用，返回false
     */
    public static boolean recoverCache(Configuration config) {
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

        DnsCacheKey cacheKey = DnsCacheKey.toCacheKey(dnscache);
        if (cacheKey == null)
            return true;

        String currentTime = String.valueOf(System.currentTimeMillis());
        String localip = AndroidNetwork.getHostIP();

        if (currentTime == null || localip == null)
            return true;
        long cacheTime = (Long.parseLong(currentTime) - Long.parseLong(cacheKey.getCurrentTime())) / 1000;
        if (!cacheKey.getLocalIp().equals(localip) || cacheTime > config.dnsCacheTimeMs) {
            return true;
        }
        mDnsCacheKey.set(cacheKey);
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
        String akScope = StringUtils.getAkAndScope(token);
        if (currentTime == null || localip == null || akScope == null)
            return;
        DnsCacheKey dnsCacheKey = new DnsCacheKey(currentTime, localip, akScope);
        String cacheKey = dnsCacheKey.toString();

        Recorder recorder = null;
        DnsPrefetcher dnsPrefetcher = null;
        try {
            recorder = new DnsCacheFile(Config.dnscacheDir);
            dnsPrefetcher = DnsPrefetcher.getDnsPrefetcher().init(token, config);
            //确认预取结束后，需要更新缓存mDnsCacheKey
            mDnsCacheKey.set(dnsCacheKey);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        if (config.dns != null) {
            DnsPrefetcher.getDnsPrefetcher().dnsPreByCustom(config.dns);
        }
        if (dnsPrefetcher != null) {
            ConcurrentHashMap<String, List<InetAddress>> concurrentHashMap = dnsPrefetcher.getConcurrentHashMap();
            byte[] dnscache = StringUtils.toByteArray(concurrentHashMap);
            if (dnscache == null)
                return;
            recorder.set(cacheKey, dnscache);
        }
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
        return false;
    }
}
