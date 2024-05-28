package com.qiniu.android.common;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.storage.Configuration;
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
     * <p>
     * 废弃，使用 {@link FixedZone#createWithRegionId} 替换，regionId：z0
     */
    @Deprecated
    public static final Zone zone0 = new FixedZone(new String[]{"upload.qiniup.com", "up.qiniup.com"},
            new String[]{"upload.qbox.me", "up.qbox.me"},
            "z0");

    /**
     * 华东浙江 2 机房
     * <p>
     * 废弃，使用 {@link FixedZone#createWithRegionId} 替换，regionId：cn-east-2
     */
    @Deprecated
    public static final Zone zoneCnEast2 = new FixedZone(new String[]{"upload-cn-east-2.qiniup.com", "up-cn-east-2.qiniup.com"},
            null,
            "cn-east-2");

    /**
     * 华北机房
     * <p>
     * 废弃，使用 {@link FixedZone#createWithRegionId} 替换，regionId：z1
     */
    @Deprecated
    public static final Zone zone1 = new FixedZone(new String[]{"upload-z1.qiniup.com", "up-z1.qiniup.com"},
            new String[]{"upload-z1.qbox.me", "up-z1.qbox.me"},
            "z1");

    /**
     * 华南机房
     * <p>
     * 废弃，使用 {@link FixedZone#createWithRegionId} 替换，regionId：z2
     */
    @Deprecated
    public static final Zone zone2 = new FixedZone(new String[]{"upload-z2.qiniup.com", "up-z2.qiniup.com"},
            new String[]{"upload-z2.qbox.me", "up-z2.qbox.me"},
            "z2");

    /**
     * 北美机房
     * <p>
     * 废弃，使用 {@link FixedZone#createWithRegionId} 替换，regionId：na0
     */
    @Deprecated
    public static final Zone zoneNa0 = new FixedZone(new String[]{"upload-na0.qiniup.com", "up-na0.qiniup.com"},
            new String[]{"upload-na0.qbox.me", "up-na0.qbox.me"},
            "na0");

    /**
     * 新加坡机房
     * <p>
     * 废弃，使用 {@link FixedZone#createWithRegionId} 替换，regionId：as0
     */
    @Deprecated
    public static final Zone zoneAs0 = new FixedZone(new String[]{"upload-as0.qiniup.com", "up-as0.qiniup.com"},
            new String[]{"upload-as0.qbox.me", "up-as0.qbox.me"},
            "as0");

    /**
     * FixedZone 构造方法
     * regionId 参考链接：https://developer.qiniu.com/kodo/1671/region-endpoint-fq
     *
     * @param regionId 根据区域 ID 创建 Zone
     * @return Zone 实例
     */
    public static FixedZone createWithRegionId(String regionId) {
        String[] upDomains = new String[]{
                "upload-" + regionId + ".qiniup.com",
                "up-" + regionId + ".qiniup.com"
        };
        return new FixedZone(upDomains, new String[]{}, regionId);
    }

    private ZonesInfo zonesInfo;

    /**
     * 获取 SDK 中使用的 zone，用于重试，外部不可使用
     *
     * @return FixedZone
     */
    @Deprecated
    public static FixedZone localsZoneInfo() {
        FixedZone[] localsZone = new FixedZone[]{
                (FixedZone) zone0, (FixedZone) zoneCnEast2,
                (FixedZone) zone1, (FixedZone) zone2,
                (FixedZone) zoneNa0,
                (FixedZone) zoneAs0,
        };

        FixedZone zone = combineZones(localsZone);
        if (zone != null) {
            zone.zonesInfo.toTemporary();
        }
        return zone;
    }

    /**
     * 把多个 Zone 合并成一个 Zone Group，重试时会依次切换不同 Zone 进行重试
     *
     * @param zones zones
     * @return Zone Group
     */
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

    /**
     * 构造方法
     *
     * @param zoneInfo zone 信息
     */
    public FixedZone(ZoneInfo zoneInfo) {
        ArrayList<ZoneInfo> zoneInfoList = new ArrayList<>();
        zoneInfoList.add(zoneInfo);
        this.zonesInfo = new ZonesInfo(zoneInfoList);
    }

    /**
     * 构造方法
     *
     * @param zonesInfo zones 信息
     */
    public FixedZone(ZonesInfo zonesInfo) {
        this.zonesInfo = zonesInfo;
    }

    /**
     * 构造方法
     *
     * @param upDomains 上传域名
     */
    public FixedZone(String[] upDomains) {
        this(upDomains, null);
    }

    /**
     * 构造方法
     *
     * @param upDomains 上传域名
     * @param regionId  区域 ID
     */
    public FixedZone(String[] upDomains, String regionId) {
        this(upDomains, null, regionId);
    }

    private FixedZone(String[] upDomains, String[] oldUpDomains, String regionId) {
        this.zonesInfo = createZonesInfo(upDomains, oldUpDomains, regionId);
    }

    public FixedZone(String[] accUpDomains,String[] upDomains, String[] oldUpDomains, String regionId) {
        this.zonesInfo = createZonesInfo(accUpDomains, upDomains, oldUpDomains, regionId);
    }

    private ZonesInfo createZonesInfo(String[] upDomains,
                                      String[] oldUpDomains,
                                      String regionId) {
        return createZonesInfo(null, upDomains, oldUpDomains, regionId);
    }

    private ZonesInfo createZonesInfo(String[] accDomains,
                                      String[] upDomains,
                                      String[] oldUpDomains,
                                      String regionId) {

        if ((accDomains == null || accDomains.length == 0) &&
                (upDomains == null || upDomains.length == 0)) {
            return null;
        }

        List<String> accDomainsList = new ArrayList<>();
        List<String> upDomainsList = new ArrayList<>();
        List<String> oldUpDomainsList = new ArrayList<>();
        if (accDomains != null && accDomains.length > 0) {
            accDomainsList = new ArrayList<>(Arrays.asList(accDomains));
        }
        if (upDomains != null && upDomains.length > 0) {
            upDomainsList = new ArrayList<>(Arrays.asList(upDomains));
        }
        if (oldUpDomains != null && oldUpDomains.length > 0) {
            oldUpDomainsList = new ArrayList<>(Arrays.asList(oldUpDomains));
        }

        ZoneInfo zoneInfo = ZoneInfo.buildInfo(accDomainsList, upDomainsList, oldUpDomainsList, regionId);
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

    @Override
    public void query(Configuration configuration, UpToken token, QueryHandlerV2 completeHandler) {
        if (completeHandler != null) {
            completeHandler.complete(ResponseInfo.successResponse(), null, zonesInfo);
        }
    }
}
