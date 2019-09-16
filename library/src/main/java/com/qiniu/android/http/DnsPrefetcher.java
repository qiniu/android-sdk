package com.qiniu.android.http;

import com.qiniu.android.collect.Config;
import com.qiniu.android.common.Constants;
import com.qiniu.android.common.FixedZone;
import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.utils.UrlSafeBase64;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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
}
