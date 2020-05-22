package com.qiniu.android.http.request;

import com.qiniu.android.common.ZoneInfo;

import org.jetbrains.annotations.NotNull;

public interface UploadRegion {

    public ZoneInfo getZoneInfo();

    public void setupRegionData(@NotNull ZoneInfo zoneInfo);

    public UploadServerInterface getNextServer(boolean isOldServer, UploadServerInterface freezeServer);
}
