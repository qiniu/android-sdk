package com.qiniu.android.common;

import com.qiniu.android.storage.UpToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Created by long on 2016/9/29.
 */

public final class FixedZone extends Zone {
    /**
     * 华东机房
     */
    public static final Zone zone0 = new FixedZone(new String[]{"upload.qiniup.com", "up.qiniup.com"},
            new String[]{"upload.qbox.me", "up.qbox.me"},
            "z0");

    /**
     * 华东浙江 2 机房
     */
    public static final Zone zoneCnEast2 = new FixedZone(new String[]{"upload-cn-east-2.qiniup.com", "up-cn-east-2.qiniup.com"},
            null,
            "cn-east-2");

    /**
     * 华北机房
     */
    public static final Zone zone1 = new FixedZone(new String[]{"upload-z1.qiniup.com", "up-z1.qiniup.com"},
            new String[]{"upload-z1.qbox.me", "up-z1.qbox.me"},
            "z1");

    /**
     * 华南机房
     */
    public static final Zone zone2 = new FixedZone(new String[]{"upload-z2.qiniup.com", "up-z2.qiniup.com"},
            new String[]{"upload-z2.qbox.me", "up-z2.qbox.me"},
            "z2");

    /**
     * 首尔机房
     */
    public static final Zone zoneApNorthEast1 = new FixedZone(new String[]{"upload-ap-northeast-1.qiniup.com", "up-ap-northeast-1.qiniup.com"},
            null,
            "ap-northeast-1");

    /**
     * 北美机房
     */
    public static final Zone zoneNa0 = new FixedZone(new String[]{"upload-na0.qiniup.com", "up-na0.qiniup.com"},
            new String[]{"upload-na0.qbox.me", "up-na0.qbox.me"},
            "na0");

    /**
     * 新加坡机房
     */
    public static final Zone zoneAs0 = new FixedZone(new String[]{"upload-as0.qiniup.com", "up-as0.qiniup.com"},
            new String[]{"upload-as0.qbox.me", "up-as0.qbox.me"},
            "as0");

    private ZonesInfo zonesInfo;

    public static FixedZone localsZoneInfo() {
        FixedZone[] localsZone = new FixedZone[]{
                (FixedZone) zone0, (FixedZone) zoneCnEast2,
                (FixedZone) zone1, (FixedZone) zone2,
                (FixedZone) zoneNa0, (FixedZone) zoneApNorthEast1,
                (FixedZone) zoneAs0,
        };

        FixedZone zone = combineZones(localsZone);
        if (zone != null) {
            zone.zonesInfo.toTemporary();
        }
        return zone;
    }

    public static FixedZone combineZones(FixedZone[] zones) {
        if (zones == null || zones.length == 0) {
            return null;
        }

        ArrayList<ZoneInfo> zoneInfoArray = new ArrayList<>();
        for (FixedZone zone : zones) {
            if (zone.zonesInfo != null && zone.zonesInfo.zonesInfo != null) {
                zoneInfoArray.addAll(zone.zonesInfo.zonesInfo);
            }
        }

        ZonesInfo zonesInfo = new ZonesInfo(zoneInfoArray, true);
        return new FixedZone(zonesInfo);
    }

    public FixedZone(ZoneInfo zoneInfo) {
        ArrayList<ZoneInfo> zoneInfoList = new ArrayList<>();
        zoneInfoList.add(zoneInfo);
        this.zonesInfo = new ZonesInfo(zoneInfoList);
    }

    public FixedZone(ZonesInfo zonesInfo) {
        this.zonesInfo = zonesInfo;
    }

    public FixedZone(String[] upDomains) {
        this(upDomains, null);
    }

    public FixedZone(String[] upDomains, String regionId) {
        this(upDomains, null, regionId);
    }

    private FixedZone(String[] upDomains, String[] oldUpDomains, String regionId) {
        this.zonesInfo = createZonesInfo(upDomains, oldUpDomains, regionId);
    }

    private ZonesInfo createZonesInfo(String[] upDomains,
                                      String[] oldUpDomains,
                                      String regionId) {

        if (upDomains == null || upDomains.length == 0) {
            return null;
        }

        List<String> upDomainsList = new ArrayList<String>(Arrays.asList(upDomains));
        List<String> oldUpDomainsList = null;
        if (oldUpDomains != null && oldUpDomains.length > 0) {
            oldUpDomainsList = new ArrayList<String>(Arrays.asList(oldUpDomains));
        } else {
            oldUpDomainsList = new ArrayList<>();
        }

        ZoneInfo zoneInfo = ZoneInfo.buildInfo(upDomainsList, oldUpDomainsList, regionId);
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
        if (completeHandler != null) {
            completeHandler.complete(0, null, null);
        }
    }
}
