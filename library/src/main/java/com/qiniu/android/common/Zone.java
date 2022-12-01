package com.qiniu.android.common;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.storage.UpToken;

/**
 * Created by bailong on 15/10/10.
 */
public abstract class Zone {

    public abstract ZonesInfo getZonesInfo(UpToken token);

    public abstract void preQuery(UpToken token, QueryHandler completeHandler);

    public interface QueryHandler {
        void complete(int code, ResponseInfo responseInfo, UploadRegionRequestMetrics metrics);
    }
}
