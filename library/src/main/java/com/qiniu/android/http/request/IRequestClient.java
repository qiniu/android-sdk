package com.qiniu.android.http.request;


import com.qiniu.android.http.ProxyConfiguration;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadSingleRequestMetrics;

import org.json.JSONObject;


public interface IRequestClient {

    public interface RequestClientProgress {
        public void progress(long totalBytesWritten, long totalBytesExpectedToWrite);
    }

    public interface RequestClientCompleteHandler {
        public void complete(ResponseInfo responseInfo, UploadSingleRequestMetrics metrics, JSONObject response);
    }

    public void request(Request request,
                        boolean isAsync,
                        ProxyConfiguration connectionProxy,
                        RequestClientProgress progress,
                        RequestClientCompleteHandler complete);

    public void cancel();
}
