package com.qiniu.android.http.request;

import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.http.ResponseInfo;

public interface IUploadRegion {

    boolean isValid();

    boolean isEqual(IUploadRegion region);

    ZoneInfo getZoneInfo();

    void setupRegionData(ZoneInfo zoneInfo);

    IUploadServer getNextServer(boolean isOldServer, ResponseInfo responseInfo, IUploadServer freezeServer);
}
