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
    static String[] arrayzone0 = new String[]{
            "upload.qiniup.com", "upload-jjh.qiniup.com",
            "upload-xs.qiniup.com", "up.qiniup.com",
            "up-jjh.qiniup.com", "up-xs.qiniup.com",
            "upload.qbox.me", "up.qbox.me"
    };
    public static final Zone zone0 = new FixedZone(arrayzone0, new String[]{"iovip.qbox.me"});

    /**
     * 华北机房
     */
    static String[] arrayzone1 = new String[]{
            "upload-z1.qiniup.com", "up-z1.qiniup.com",
            "upload-z1.qbox.me", "up-z1.qbox.me"
    };
    public static final Zone zone1 = new FixedZone(arrayzone1, new String[]{"iovip-z1.qbox.me"});

    /**
     * 华南机房
     */
    static String[] arrayzone2 = new String[]{
            "upload-z2.qiniup.com", "upload-dg.qiniup.com",
            "upload-fs.qiniup.com", "up-z2.qiniup.com",
            "up-dg.qiniup.com", "up-fs.qiniup.com",
            "upload-z2.qbox.me", "up-z2.qbox.me"
    };
    public static final Zone zone2 = new FixedZone(arrayzone2, new String[]{"iovip-z2.qbox.me"});

    /**
     * 北美机房
     */
    static String[] arrayzoneNa0 = new String[]{
            "upload-na0.qiniup.com", "up-na0.qiniup.com",
            "upload-na0.qbox.me", "up-na0.qbox.me"
    };
    public static final Zone zoneNa0 = new FixedZone(arrayzoneNa0, new String[]{"iovip-na0.qbox.me"});

    /**
     * 新加坡机房
     */
    static String[] arrayZoneAs0 = new String[]{
            "upload-as0.qiniup.com", "up-as0.qiniup.com",
            "upload-as0.qbox.me", "up-as0.qbox.me"
    };
    public static final Zone zoneAs0 = new FixedZone(arrayZoneAs0, new String[]{"iovip-as0.qbox.me"});

    private ZonesInfo zonesInfo;

    public static List<Zone> localsZoneInfo() {
        ArrayList<Zone> localsZone = new ArrayList<Zone>();
        localsZone.add(zone0);
        localsZone.add(zone1);
        localsZone.add(zone2);
        localsZone.add(zoneNa0);
        localsZone.add(zoneAs0);
        return localsZone;
    }

    public FixedZone(ZoneInfo zoneInfo) {
        ArrayList<ZoneInfo> zoneInfos = new ArrayList<>();
        zoneInfos.add(zoneInfo);
        this.zonesInfo = new ZonesInfo(zoneInfos);
    }

    public FixedZone(String[] upDomains) {
        this(upDomains, null);
    }

    public FixedZone(String[] upDomains, String[] ioDomains) {
        this.zonesInfo = createZonesInfo(upDomains, ioDomains);
    }

    private ZonesInfo createZonesInfo(String[] upDomains, String[] ioDomains) {
        if (upDomains == null || upDomains.length == 0) {
            return null;
        }

        ArrayList<String> upDomainsList = new ArrayList<String>(Arrays.asList(upDomains));
        ArrayList<String> ioDomainsList = null;
        if (ioDomains != null){
            ioDomainsList = new ArrayList<String>(Arrays.asList(ioDomains));
        }
        ZoneInfo zoneInfo = ZoneInfo.buildInfo(upDomainsList, ioDomainsList);
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
