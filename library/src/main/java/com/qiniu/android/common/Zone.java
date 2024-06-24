package com.qiniu.android.common;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpToken;

/**
 * Created by bailong on 15/10/10.
 */
public abstract class Zone {

    /**
     * 构造函数
     */
    protected Zone() {
    }

    /**
     * 根据上传 token 获取 zone
     *
     * @param token 上传 token
     * @return 区域信息
     */
    public abstract ZonesInfo getZonesInfo(UpToken token);

    /**
     * 根据上传 token 对区域进行预查询
     *
     * @param token           上传 token
     * @param completeHandler 预查询结束回调
     */
    @Deprecated
    public abstract void preQuery(UpToken token, QueryHandler completeHandler);

    /**
     * 根据上传 token 对区域进行预查询
     *
     * @param configuration   配置信息
     * @param token           上传 token
     * @param completeHandler 预查询结束回调
     */
    public abstract void query(Configuration configuration, UpToken token, final QueryHandlerV2 completeHandler);

    /**
     * 预查询结束回调
     */
    public interface QueryHandler {
        /**
         * 预查询结束回调
         *
         * @param code         状态码
         * @param responseInfo 查询响应
         * @param metrics      查询指标
         */
        void complete(int code, ResponseInfo responseInfo, UploadRegionRequestMetrics metrics);
    }

    /**
     * 预查询结束回调
     */
    public interface QueryHandlerV2 {
        /**
         * 预查询结束回调
         *
         * @param responseInfo 查询响应
         * @param metrics      查询指标
         * @param zonesInfo    区域信息
         */
        void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics metrics, ZonesInfo zonesInfo);
    }
}
