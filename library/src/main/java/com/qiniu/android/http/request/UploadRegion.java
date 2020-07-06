package com.qiniu.android.http.request;

import com.qiniu.android.common.ZoneInfo;

public interface UploadRegion {

    public ZoneInfo getZoneInfo();

    public void setupRegionData(ZoneInfo zoneInfo);

    public IUploadServer getNextServer(boolean isOldServer, IUploadServer freezeServer);
}
