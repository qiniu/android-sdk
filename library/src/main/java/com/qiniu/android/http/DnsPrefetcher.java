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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * <p>
 * Created by jemy on 2019/8/20.
 */

public class DnsPrefetcher {

    public static DnsPrefetcher dnsPrefetcher = null;
    private static String token;

    private static HashMap<String, List<InetAddress>> mConcurrentHashMap = new HashMap<String, List<InetAddress>>();
    private List<String> mHosts = new ArrayList<String>();


    private DnsPrefetcher() {

    }

    public static DnsPrefetcher getDnsPrefetcher() {
        if (dnsPrefetcher == null) {
            synchronized (dnsPrefetcher) {
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

    public void setConcurrentHashMap(HashMap<String, List<InetAddress>> mConcurrentHashMap) {
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
    public HashMap<String, List<InetAddress>> getConcurrentHashMap() {
        return this.mConcurrentHashMap;
    }

    //use for test
    public void setToken(String token) {
        this.token = token;
    }

    public List<InetAddress> getInetAddressByHost(String host) {
        return mConcurrentHashMap.get(host);
    }


    public void preHosts() {
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
        mHosts.add(Config.preQueryHost);
    }


    public void preFetch() {
        for (int i = 0; i < mHosts.size(); i++) {
            List<InetAddress> inetAddresses = null;
            try {
                inetAddresses = getDnsBySystem().lookup(mHosts.get(i));
                mConcurrentHashMap.put(mHosts.get(i), inetAddresses);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

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
        for (int i = 0; i < mHosts.size(); i++) {
            List<InetAddress> inetAddresses = null;
            try {
                inetAddresses = dns.lookup(mHosts.get(i));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            mConcurrentHashMap.put(mHosts.get(i), inetAddresses);
        }
    }


    /**
     * 系统DNS解析预取
     *
     * @return
     * @throws UnknownHostException
     */
    public Dns getDnsBySystem() throws UnknownHostException {
        return new SystemDns();
    }


    class SystemDns implements Dns {
        @Override
        public List<InetAddress> lookup(String hostname) throws UnknownHostException {
            return okhttp3.Dns.SYSTEM.lookup(hostname);
        }
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
