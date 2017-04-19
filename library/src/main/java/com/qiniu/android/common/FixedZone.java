package com.qiniu.android.common;

import java.util.ArrayList;
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
    public static final Zone zone0 = new FixedZone(createZoneInfo(new String[]{
            "upload1.qiniup.com", "upload-nb.qiniup.com",
            "upload-xs.qiniup.com", "up.qiniup.com",
            "up-nb.qiniup.com", "up-xs.qiniup.com",
            "upload.qbox.me", "up.qbox.me"
    }));

    /**
     * 华北机房
     */
    public static final Zone zone1 = new FixedZone(createZoneInfo(new String[]{
            "upload-z1.qiniup.com", "up-z1.qiniup.com",
            "upload-z1.qbox.me", "up-z1.qbox.me"
    }));

    /**
     * 华南机房
     */
    public static final Zone zone2 = new FixedZone(createZoneInfo(new String[]{
            "upload-z2.qiniup.com", "upload-gz.qiniup.com",
            "upload-fs.qiniup.com", "up-z2.qiniup.com",
            "up-gz.qiniup.com", "up-fs.qiniup.com",
            "upload-z2.qbox.me", "up-z2.qbox.me"
    }));

    /**
     * 北美机房
     */
    public static final Zone zoneNa0 = new FixedZone(createZoneInfo(new String[]{
            "upload-na0.qiniu.com", "up-na0.qiniup.com",
            "upload-na0.qbox.me", "up-na0.qbox.me"
    }));

    private ZoneInfo zoneInfo;
    public FixedZone(ZoneInfo zoneInfo) {
        this.zoneInfo = zoneInfo;
    }

    public FixedZone(String[] upDomains){
        this.zoneInfo=createZoneInfo(upDomains);
    }

    private static ZoneInfo createZoneInfo(String[] upDomains) {
        List<String> upDomainsList = new ArrayList<String>();
        Map<String, Long> upDomainsMap = new ConcurrentHashMap<String, Long>();
        for (String domain : upDomains) {
            upDomainsList.add(domain);
            upDomainsMap.put(domain, 0L);
        }
        return new ZoneInfo(0, upDomainsList, upDomainsMap);
    }

    @Override
    public synchronized String upHost(String upToken, boolean useHttps,String frozenDomain) {
        return this.upHost(this.zoneInfo, useHttps,frozenDomain);
    }

    @Override
    public void preQuery(String token, QueryHandler complete) {
        complete.onSuccess();
    }
}
