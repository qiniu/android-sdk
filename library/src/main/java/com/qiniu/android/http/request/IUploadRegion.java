package com.qiniu.android.http.request;

import com.qiniu.android.common.ZoneInfo;

public interface IUploadRegion {

    ZoneInfo getZoneInfo();

    void setupRegionData(ZoneInfo zoneInfo);

    IUploadServer getNextServer(boolean isOldServer, IUploadServer freezeServer);
}
