package com.qiniu.android.common;

import android.util.Log;

import com.qiniu.android.storage.UpToken;

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
    public static final Zone zone0 = new FixedZone(new String[]{"upload.qiniup.com", "up.qiniup.com"},
            new String[]{"upload.qbox.me", "up.qbox.me"},
            new String[]{"iovip.qbox.me"});

    /**
     * 华北机房
     */
    public static final Zone zone1 = new FixedZone(new String[]{"upload-z1.qiniup.com", "up-z1.qiniup.com"},
            new String[]{"upload-z1.qbox.me", "up-z1.qbox.me"},
            new String[]{"iovip-z1.qbox.me"});

    /**
     * 华南机房
     */
    public static final Zone zone2 = new FixedZone(new String[]{"upload-z2.qiniup.com", "up-z2.qiniup.com"},
            new String[]{"upload-z2.qbox.me", "up-z2.qbox.me"},
            new String[]{"iovip-z2.qbox.me"});

    /**
     * 北美机房
     */
    public static final Zone zoneNa0 = new FixedZone(new String[]{"upload-na0.qiniup.com", "up-na0.qiniup.com"},
            new String[]{"upload-na0.qbox.me", "up-na0.qbox.me"},
            new String[]{"iovip-na0.qbox.me"});

    /**
     * 新加坡机房
     */
    public static final Zone zoneAs0 = new FixedZone(new String[]{"upload-as0.qiniup.com", "up-as0.qiniup.com"},
            new String[]{"upload-as0.qbox.me", "up-as0.qbox.me"},
            new String[]{"iovip-as0.qbox.me"});

    private ZonesInfo zonesInfo;

    public static FixedZone localsZoneInfo() {
        ArrayList<FixedZone> localsZone = new ArrayList<>();
        localsZone.add((FixedZone)zone0);
        localsZone.add((FixedZone)zone1);
        localsZone.add((FixedZone)zone2);
        localsZone.add((FixedZone)zoneNa0);
        localsZone.add((FixedZone)zoneAs0);

        ArrayList<ZoneInfo> zoneInfoArray = new ArrayList<>();
        for (FixedZone zone : localsZone){
            if (zone.zonesInfo != null && zone.zonesInfo.zonesInfo != null){
                zoneInfoArray.addAll(zone.zonesInfo.zonesInfo);
            }
        }

        ZonesInfo zonesInfo = new ZonesInfo(zoneInfoArray);
        return new FixedZone(zonesInfo);
    }

    public FixedZone(ZoneInfo zoneInfo) {
        ArrayList<ZoneInfo> zoneInfos = new ArrayList<>();
        zoneInfos.add(zoneInfo);
        this.zonesInfo = new ZonesInfo(zoneInfos);
    }

    public FixedZone(ZonesInfo zonesInfo){
        this.zonesInfo = zonesInfo;
    }

    public FixedZone(String[] upDomains) {
        this(upDomains, null);
    }

    public FixedZone(String[] upDomains, String[] ioDomains) {
        this(upDomains, null, ioDomains);
    }

    private FixedZone(String[] upDomains, String[] oldUpDomains, String[] ioDomains) {
        this.zonesInfo = createZonesInfo(upDomains, oldUpDomains, ioDomains);
    }

    private ZonesInfo createZonesInfo(String[] upDomains,
                                      String[] oldUpDomains,
                                      String[] ioDomains) {

        if (upDomains == null || upDomains.length == 0) {
            return null;
        }

        ArrayList<String> upDomainsList = new ArrayList<String>(Arrays.asList(upDomains));
        ArrayList<String> oldUpDomainsList = null;
        if (oldUpDomains != null){
            oldUpDomainsList = new ArrayList<String>(Arrays.asList(oldUpDomains));
        }
        ArrayList<String> ioDomainsList = null;
        if (ioDomains != null){
            ioDomainsList = new ArrayList<String>(Arrays.asList(ioDomains));
        }
        ZoneInfo zoneInfo = ZoneInfo.buildInfo(upDomainsList, oldUpDomainsList, ioDomainsList);
        if (zoneInfo == null) {
            return null;
        }
        ArrayList<ZoneInfo> zoneInfoList = new ArrayList<ZoneInfo>();
        zoneInfoList.add(zoneInfo);
        return new ZonesInfo(zoneInfoList);
    }

    @Override
    public ZonesInfo getZonesInfo(UpToken token) {
        return zonesInfo;
    }

    @Override
    public void preQuery(UpToken token, QueryHandler completeHandler) {
        if (completeHandler != null){
            completeHandler.complete(0, null, null);
        }
    }
}
