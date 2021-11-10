package com.qiniu.android.http.request;

import com.qiniu.android.http.ProxyConfiguration;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadSingleRequestMetrics;

import org.json.JSONObject;


public abstract class IRequestClient {

    public interface RequestClientProgress {
        void progress(long totalBytesWritten, long totalBytesExpectedToWrite);
    }

    public interface RequestClientCompleteHandler {
        void complete(ResponseInfo responseInfo, UploadSingleRequestMetrics metrics, JSONObject response);
    }

    public abstract void request(Request request,
                                 boolean isAsync,
                                 ProxyConfiguration connectionProxy,
                                 RequestClientProgress progress,
                                 RequestClientCompleteHandler complete);

    public abstract void cancel();
}
