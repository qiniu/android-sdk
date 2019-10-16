package com.qiniu.android.common;

import android.util.Log;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by long on 2016/9/29.
 */

public final class FixedZone extends Zone {
    /**
     * 华东机房
     */
    static String[] arrayzone0 = new String[]{
            "upload.qiniup.com", "upload-jjh.qiniup.com",
            "upload-xs.qiniup.com", "up.qiniup.com",
            "up-jjh.qiniup.com", "up-xs.qiniup.com",
            "upload.qbox.me", "up.qbox.me"
    };
    public static final Zone zone0 = new FixedZone(arrayzone0);

    /**
     * 华北机房
     */
    static String[] arrayzone1 = new String[]{
            "upload-z1.qiniup.com", "up-z1.qiniup.com",
            "upload-z1.qbox.me", "up-z1.qbox.me"
    };
    public static final Zone zone1 = new FixedZone(arrayzone1);

    /**
     * 华南机房
     */
    static String[] arrayzone2 = new String[]{
            "upload-z2.qiniup.com", "upload-dg.qiniup.com",
            "upload-fs.qiniup.com", "up-z2.qiniup.com",
            "up-dg.qiniup.com", "up-fs.qiniup.com",
            "upload-z2.qbox.me", "up-z2.qbox.me"
    };
    public static final Zone zone2 = new FixedZone(arrayzone2);

    /**
     * 北美机房
     */
    static String[] arrayzoneNa0 = new String[]{
            "upload-na0.qiniup.com", "up-na0.qiniup.com",
            "upload-na0.qbox.me", "up-na0.qbox.me"
    };
    public static final Zone zoneNa0 = new FixedZone(arrayzoneNa0);

    /**
     * 新加坡机房
     */
    static String[] arrayZoneAs0 = new String[]{
            "upload-as0.qiniup.com", "up-as0.qiniup.com",
            "upload-as0.qbox.me", "up-as0.qbox.me"
    };
    public static final Zone zoneAs0 = new FixedZone(arrayZoneAs0);

    private ZoneInfo zoneInfo;

    public static List<ZoneInfo> getZoneInfos() {
        List<ZoneInfo> listZoneInfo = new ArrayList<ZoneInfo>();
        listZoneInfo.add(createZoneInfo(arrayzone0));
        listZoneInfo.add(createZoneInfo(arrayzone1));
        listZoneInfo.add(createZoneInfo(arrayzone2));
        listZoneInfo.add(createZoneInfo(arrayzoneNa0));
        listZoneInfo.add(createZoneInfo(arrayZoneAs0));
        return listZoneInfo;
    }

    public FixedZone(ZoneInfo zoneInfo) {
        this.zoneInfo = zoneInfo;
    }

    public FixedZone(String[] upDomains) {
        this.zoneInfo = createZoneInfo(upDomains);
    }

    public static ZoneInfo createZoneInfo(String[] upDomains) {
        List<String> upDomainsList = new ArrayList<String>();
        Map<String, Long> upDomainsMap = new ConcurrentHashMap<String, Long>();
        for (String domain : upDomains) {
            upDomainsList.add(domain);
            upDomainsMap.put(domain, 0L);
        }
        return new ZoneInfo(0, upDomainsList, upDomainsMap);
    }


    @Override
    public synchronized String upHost(String upToken, boolean useHttps, String frozenDomain) {
        String upHost = this.upHost(this.zoneInfo, useHttps, frozenDomain);
        for (Map.Entry<String, Long> entry : this.zoneInfo.upDomainsMap.entrySet()) {
            Log.d("Qiniu.FixedZone", entry.getKey() + ", " + entry.getValue());
        }
        return upHost;
    }

    @Override
    public void preQuery(String token, QueryHandler complete) {
        complete.onSuccess();
    }

    @Override
    public boolean preQuery(String token) {
        return true;
    }

    @Override
    public ZoneInfo getZoneInfo(String token) {
        return this.zoneInfo;
    }

    @Override
    public synchronized void frozenDomain(String upHostUrl) {
        if (upHostUrl != null) {
            URI uri = URI.create(upHostUrl);
            //frozen domain
            String frozenDomain = uri.getHost();
            zoneInfo.frozenDomain(frozenDomain);
        }
    }
}
