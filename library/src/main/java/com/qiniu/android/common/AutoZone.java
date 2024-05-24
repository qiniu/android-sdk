package com.qiniu.android.common;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.dns.DnsPrefetchTransaction;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.http.request.RequestTransaction;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.utils.Cache;
import com.qiniu.android.utils.ListUtils;
import com.qiniu.android.utils.SingleFlight;
import com.qiniu.android.utils.UrlSafeBase64;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by long on 2016/9/29.
 */
public final class AutoZone extends Zone {
    /**
     * 自动判断机房
     */
    private String[] ucServers;
    private final List<RequestTransaction> transactions = new ArrayList<>();
    private FixedZone defaultZone;

    /**
     * 已经查到的 zones
     */
    private final Map<String, ZonesInfo> zonesInfoMap = new ConcurrentHashMap<>();
    private static final SingleFlight<SingleFlightValue> SingleFlight = new SingleFlight<>();

    private static final Cache zoneCache = new Cache.Builder(ZonesInfo.class)
            .setVersion("v1")
            .builder();

    /**
     * 构造函数
     */
    public AutoZone() {
    }

    /**
     * 配置 UC 域名，此 UC 域名用于根据上传 bucket 查询对应的区域。
     * 公有云不用配置
     *
     * @param ucServer UC 域名
     */
    public void setUcServer(String ucServer) {
        if (ucServer != null) {
            this.ucServers = new String[]{ucServer};
        }
    }

    /**
     * 配置 UC 域名，此 UC 域名用于根据上传 bucket 查询对应的区域。
     * 公有云不用配置
     *
     * @param ucServers UC 域名，可以是多个，多个会进行主备重试
     */
    public void setUcServers(String[] ucServers) {
        if (ucServers != null && ucServers.length > 0) {
            this.ucServers = ucServers;
        }
    }

    /**
     * 配置默认区域，在使用 AutoZone 进行上传时，当根据 bucket 查询 Zone 信息失败时如果默认区域存在会尝试使用默认区域进行上传。
     *
     * @param zones 默认区域，可以是多个
     */
    public void setDefaultZones(FixedZone[] zones) {
        defaultZone = FixedZone.combineZones(zones);
    }

    /**
     * 获取 UC 域名列表
     *
     * @return UC 域名列表
     */
    public List<String> getUcServerList() {
        if (ucServers != null && ucServers.length > 0) {
            ArrayList<String> serverList = new ArrayList<>();
            Collections.addAll(serverList, ucServers);
            return serverList;
        } else {
            return Arrays.asList(Config.preQueryHosts());
        }
    }

    /**
     * 清除区域存储缓存
     */
    public static void clearCache() {
        zoneCache.clearMemoryCache();
        zoneCache.clearDiskCache();
    }

    private String[] getUcServerArray() {
        if (ucServers != null && ucServers.length > 0) {
            return ucServers;
        } else {
            return Config.preQueryHosts();
        }
    }

    @Override
    @Deprecated
    public ZonesInfo getZonesInfo(UpToken token) {
        if (token == null) {
            return null;
        }
        String cacheKey = makeCacheKey(null, token.index());
        ZonesInfo zonesInfo = zonesInfoMap.get(cacheKey);
        if (zonesInfo != null) {
            try {
                zonesInfo = (ZonesInfo) zonesInfo.clone();
            } catch (Exception ignore) {
            }
        }
        return zonesInfo;
    }

    @Override
    public void preQuery(final UpToken token, final QueryHandler completeHandler) {
        preQuery(null, token, new QueryHandlerV2() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics metrics, ZonesInfo zonesInfo) {
                if (completeHandler != null) {
                    int code = ResponseInfo.NetworkError;
                    if (responseInfo != null) {
                        if (responseInfo.isOK()) {
                            code = 0;
                        } else {
                            code = responseInfo.statusCode;
                        }
                    }
                    completeHandler.complete(code, responseInfo, metrics);
                }
            }
        });
    }

    @Override
    public void preQuery(Configuration configuration, UpToken token, QueryHandlerV2 completeHandler) {
        if (token == null || !token.isValid()) {
            completeHandler.complete(ResponseInfo.invalidToken("invalid token"), null, null);
            return;
        }

        UploadRegionRequestMetrics localMetrics = new UploadRegionRequestMetrics(null);
        localMetrics.start();

        final String cacheKey = makeCacheKey(configuration, token.index());

        ZonesInfo zonesInfo = null;
        Cache.Object object = zoneCache.cacheForKey(cacheKey);
        if (object instanceof ZonesInfo) {
            zonesInfo = (ZonesInfo) object;
        }

        if (zonesInfo != null && zonesInfo.isValid()) {
            localMetrics.end();
            zonesInfoMap.put(cacheKey, zonesInfo);
            completeHandler.complete(ResponseInfo.successResponse(), localMetrics, zonesInfo);
            return;
        }

        DnsPrefetchTransaction.addDnsCheckAndPrefetchTransaction(getUcServerArray());

        final ZonesInfo finalZonesInfo = zonesInfo;
        try {
            SingleFlight.perform(cacheKey, new SingleFlight.ActionHandler<SingleFlightValue>() {
                @Override
                public void action(final com.qiniu.android.utils.SingleFlight.CompleteHandler<SingleFlightValue> completeHandler) throws Exception {

                    final RequestTransaction transaction = createUploadRequestTransaction(configuration, token);
                    transaction.queryUploadHosts(true, new RequestTransaction.RequestCompleteHandler() {
                        @Override
                        public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                            destroyUploadRequestTransaction(transaction);

                            SingleFlightValue value = new SingleFlightValue();
                            value.responseInfo = responseInfo;
                            value.response = response;
                            value.metrics = requestMetrics;
                            completeHandler.complete(value);
                        }
                    });
                }

            }, new SingleFlight.CompleteHandler<SingleFlightValue>() {
                @Override
                public void complete(SingleFlightValue singleFlightValue) {
                    ResponseInfo responseInfo = singleFlightValue.responseInfo;
                    UploadRegionRequestMetrics requestMetrics = singleFlightValue.metrics;
                    JSONObject response = singleFlightValue.response;

                    if (responseInfo != null && responseInfo.isOK() && response != null) {
                        ZonesInfo zonesInfoP = ZonesInfo.createZonesInfo(response);
                        if (zonesInfoP.isValid()) {
                            zoneCache.cache(cacheKey, zonesInfoP, true);
                            zonesInfoMap.put(cacheKey, zonesInfoP);
                            completeHandler.complete(responseInfo, requestMetrics, zonesInfoP);
                        } else {
                            responseInfo = ResponseInfo.parseError("origin response:" + responseInfo);
                            completeHandler.complete(responseInfo, requestMetrics, null);
                        }
                    } else {
                        if (defaultZone != null) {
                            // 备用只能用一次
                            ZonesInfo defaultZoneZonesInfo = defaultZone.getZonesInfo(token);
                            zonesInfoMap.put(cacheKey, defaultZoneZonesInfo);
                            String message = "origin response:" + responseInfo;
                            responseInfo = ResponseInfo.successResponse();
                            responseInfo.message = message;
                            completeHandler.complete(responseInfo, requestMetrics, defaultZoneZonesInfo);
                        } else if (finalZonesInfo != null) {
                            // 缓存有，但是失效也可使用
                            zonesInfoMap.put(cacheKey, finalZonesInfo);
                            String message = "origin response:" + responseInfo;
                            responseInfo = ResponseInfo.successResponse();
                            responseInfo.message = message;
                            completeHandler.complete(responseInfo, requestMetrics, finalZonesInfo);
                        } else {
                            completeHandler.complete(responseInfo, requestMetrics, null);
                        }
                    }
                }
            });

        } catch (Exception e) {
            /// 此处永远不会执行，回调只为占位
            completeHandler.complete(ResponseInfo.localIOError(e.toString()), null, null);
        }
    }

    private String makeCacheKey(Configuration configuration, String akAndBucket) {
        String key = akAndBucket;
        if (configuration != null) {
            key += akAndBucket + ":" + configuration.accelerateUploading;
        }
        List<String> ucHosts = getUcServerList();
        if (ListUtils.isEmpty(ucHosts)) {
            return akAndBucket;
        }

        StringBuilder hosts = new StringBuilder();
        for (String host : ucHosts) {
            if (host == null || host.isEmpty()) {
                continue;
            }
            hosts.append(host).append(":");
        }

        return UrlSafeBase64.encodeToString(hosts + key);
    }

    private RequestTransaction createUploadRequestTransaction(Configuration configuration, UpToken token) {
        List<String> hosts = getUcServerList();
        if (configuration == null) {
            configuration = new Configuration.Builder().build();
        }
        RequestTransaction transaction = new RequestTransaction(configuration, UploadOptions.defaultOptions(),
                hosts, ZoneInfo.EmptyRegionId, null, token);
        transactions.add(transaction);
        return transaction;
    }

    private void destroyUploadRequestTransaction(RequestTransaction transaction) {
        transactions.remove(transaction);
    }

    private static class SingleFlightValue {
        private ResponseInfo responseInfo;
        private JSONObject response;
        private UploadRegionRequestMetrics metrics;
    }
}
