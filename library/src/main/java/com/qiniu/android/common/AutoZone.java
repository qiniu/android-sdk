package com.qiniu.android.common;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.dns.DnsPrefetchTransaction;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.http.request.RequestTransaction;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.utils.Cache;
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
            .setVersion("v1.0.0")
            .builder();

    //私有云可能改变ucServer
    public void setUcServer(String ucServer) {
        if (ucServer != null) {
            this.ucServers = new String[]{ucServer};
        }
    }

    public void setUcServers(String[] ucServers) {
        if (ucServers != null && ucServers.length > 0) {
            this.ucServers = ucServers;
        }
    }

    public void setDefaultZones(FixedZone[] zones) {
        defaultZone = FixedZone.combineZones(zones);
    }

    public List<String> getUcServerList() {
        if (ucServers != null && ucServers.length > 0) {
            ArrayList<String> serverList = new ArrayList<>();
            Collections.addAll(serverList, ucServers);
            return serverList;
        } else {
            return Arrays.asList(Config.preQueryHosts());
        }
    }

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
    public ZonesInfo getZonesInfo(UpToken token) {
        if (token == null) {
            return null;
        }
        String cacheKey = makeCacheKey(token.index());
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
        if (token == null || !token.isValid()) {
            completeHandler.complete(-1, ResponseInfo.invalidToken("invalid token"), null);
            return;
        }

        UploadRegionRequestMetrics localMetrics = new UploadRegionRequestMetrics(null);
        localMetrics.start();

        final String cacheKey = makeCacheKey(token.index()) ;

        ZonesInfo zonesInfo = null;
        Cache.Object object = zoneCache.cacheForKey(cacheKey);
        if (object instanceof ZonesInfo) {
            zonesInfo = (ZonesInfo) object;
        }

        if (zonesInfo != null && zonesInfo.isValid()) {
            localMetrics.end();
            zonesInfoMap.put(cacheKey, zonesInfo);
            completeHandler.complete(0, ResponseInfo.successResponse(), localMetrics);
            return;
        }

        DnsPrefetchTransaction.addDnsCheckAndPrefetchTransaction(getUcServerArray());

        final ZonesInfo finalZonesInfo = zonesInfo;
        try {
            SingleFlight.perform(cacheKey, new SingleFlight.ActionHandler<SingleFlightValue>() {
                @Override
                public void action(final com.qiniu.android.utils.SingleFlight.CompleteHandler<SingleFlightValue> completeHandler) throws Exception {

                    final RequestTransaction transaction = createUploadRequestTransaction(token);
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
                            completeHandler.complete(0, responseInfo, requestMetrics);
                        } else {
                            completeHandler.complete(ResponseInfo.ParseError, responseInfo, requestMetrics);
                        }
                    } else {
                        if (defaultZone != null) {
                            // 备用只能用一次
                            ZonesInfo defaultZoneZonesInfo = defaultZone.getZonesInfo(token);
                            zonesInfoMap.put(cacheKey, defaultZoneZonesInfo);
                            completeHandler.complete(0, responseInfo, requestMetrics);
                        } else if (finalZonesInfo != null) {
                            // 缓存有，但是失效也可使用
                            zonesInfoMap.put(cacheKey, finalZonesInfo);
                            completeHandler.complete(0, responseInfo, requestMetrics);
                        } else {
                            completeHandler.complete(ResponseInfo.NetworkError, responseInfo, requestMetrics);
                        }
                    }
                }
            });

        } catch (Exception e) {
            /// 此处永远不会执行，回调只为占位
            completeHandler.complete(ResponseInfo.NetworkError, ResponseInfo.localIOError(e.toString()), null);
        }
    }

    private String makeCacheKey(String akAndBucket) {
        List<String> ucHosts = getUcServerList();
        if (ucHosts == null || ucHosts.isEmpty()) {
            return akAndBucket;
        }
        return UrlSafeBase64.encodeToString(ucHosts.get(0)+":"+akAndBucket);
    }

    private RequestTransaction createUploadRequestTransaction(UpToken token) {
        List<String> hosts = getUcServerList();

        RequestTransaction transaction = new RequestTransaction(hosts, ZoneInfo.EmptyRegionId, token);
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
