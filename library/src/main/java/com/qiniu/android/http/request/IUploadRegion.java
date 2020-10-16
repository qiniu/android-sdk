package com.qiniu.android.http.request;

import com.qiniu.android.common.ZoneInfo;

public interface IUploadRegion {

    int FrozenLevelNone = 1; // 不冻结
    int FrozenLevelPartFrozen = 1 << 1; // 局部冻结，仅影响当前文件上传的Region
    int FrozenLevelGlobalFrozen=  1 << 2; // 全局冻结

    boolean isValid();

    ZoneInfo getZoneInfo();

    void setupRegionData(ZoneInfo zoneInfo);

    IUploadServer getNextServer(boolean isOldServer, int frozenLevel, IUploadServer freezeServer);
}
