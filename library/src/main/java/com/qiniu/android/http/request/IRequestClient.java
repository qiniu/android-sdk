package com.qiniu.android.http.request;

import com.qiniu.android.http.ProxyConfiguration;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadSingleRequestMetrics;

import org.json.JSONObject;

/**
 * 请求 Client 抽象
 */
public abstract class IRequestClient {

    /**
     * 请求进度协议
     */
    public interface Progress {

        /**
         * 请求进度回调
         *
         * @param totalBytesWritten         totalBytesWritten
         * @param totalBytesExpectedToWrite totalBytesExpectedToWrite
         */
        void progress(long totalBytesWritten, long totalBytesExpectedToWrite);
    }

    /**
     * 请求完成回调
     */
    public interface CompleteHandler {

        /**
         * 请求完成回调
         *
         * @param responseInfo 请求响应信息
         * @param metrics      请求指标
         * @param response     请求响应信息
         */
        void complete(ResponseInfo responseInfo, UploadSingleRequestMetrics metrics, JSONObject response);
    }

    /**
     * 构造函数
     */
    protected IRequestClient() {
    }

    /**
     * 触发请求
     *
     * @param request  请求信息
     * @param options  可选信息
     * @param progress 进度回调
     * @param complete 完成回调
     */
    public abstract void request(Request request,
                                 Options options,
                                 Progress progress,
                                 CompleteHandler complete);

    /**
     * 取消
     */
    public abstract void cancel();

    /**
     * 获取 ClientId
     *
     * @return ClientId
     */
    public String getClientId() {
        return "customized";
    }

    /**
     * 可选信息
     */
    public static class Options {

        /**
         * 上传请求的 Server
         */
        public final IUploadServer server;

        /**
         * 是否使用异步
         */
        public final boolean isAsync;

        /**
         * 请求的代理
         */
        public final ProxyConfiguration connectionProxy;

        /**
         * 构造函数
         *
         * @param server          上传请求的 Server
         * @param isAsync         是否使用异步
         * @param connectionProxy 请求的代理
         */
        public Options(IUploadServer server, boolean isAsync, ProxyConfiguration connectionProxy) {
            this.server = server;
            this.isAsync = isAsync;
            this.connectionProxy = connectionProxy;
        }
    }
}
