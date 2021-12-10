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
                                 Options options,
                                 RequestClientProgress progress,
                                 RequestClientCompleteHandler complete);

    public abstract void cancel();

    public String getClientId() {
        return "customized";
    }

    public static class Options {
        public final IUploadServer server;
        public final boolean isAsync;
        public final ProxyConfiguration connectionProxy;

        public Options(IUploadServer server, boolean isAsync, ProxyConfiguration connectionProxy) {
            this.server = server;
            this.isAsync = isAsync;
            this.connectionProxy = connectionProxy;
        }
    }
}
