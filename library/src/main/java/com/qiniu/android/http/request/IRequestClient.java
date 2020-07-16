package com.qiniu.android.http.request;


import com.qiniu.android.http.ProxyConfiguration;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadSingleRequestMetrics;

import org.json.JSONObject;


public interface IRequestClient {

    interface RequestClientProgress {
        void progress(long totalBytesWritten, long totalBytesExpectedToWrite);
    }

    interface RequestClientCompleteHandler {
        void complete(ResponseInfo responseInfo, UploadSingleRequestMetrics metrics, JSONObject response);
    }

    void request(Request request,
                 boolean isAsync,
                 ProxyConfiguration connectionProxy,
                 RequestClientProgress progress,
                 RequestClientCompleteHandler complete);

    void cancel();
}
