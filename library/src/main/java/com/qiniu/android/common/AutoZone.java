package com.qiniu.android.common;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.dns.DnsPrefetchTransaction;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.http.request.RequestTransaction;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.utils.SingleFlight;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by long on 2016/9/29.
 */
public final class AutoZone extends Zone {
    /**
     * 自动判断机房
     */
    private String[] ucServers;
    private ArrayList<RequestTransaction> transactions = new ArrayList<>();
    private FixedZone defaultZone;

    private static final SingleFlight SingleFlight = new SingleFlight();

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
        GlobalCache.getInstance().clearCache();
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
        final String cacheKey = token.index();
        ZonesInfo zonesInfo = GlobalCache.getInstance().zonesInfoForKey(cacheKey);
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

        final String cacheKey = token.index();
        ZonesInfo zonesInfo = GlobalCache.getInstance().zonesInfoForKey(cacheKey);
        if (zonesInfo != null && zonesInfo.isValid() && !zonesInfo.isTemporary()) {
            localMetrics.end();
            completeHandler.complete(0, ResponseInfo.successResponse(), localMetrics);
            return;
        }

        DnsPrefetchTransaction.addDnsCheckAndPrefetchTransaction(getUcServerArray());

        try {
            SingleFlight.perform(cacheKey, new SingleFlight.ActionHandler() {
                @Override
                public void action(final com.qiniu.android.utils.SingleFlight.CompleteHandler completeHandler) throws Exception {

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

            }, new SingleFlight.CompleteHandler() {
                @Override
                public void complete(Object value) {
                    SingleFlightValue singleFlightValue = (SingleFlightValue) value;
                    ResponseInfo responseInfo = singleFlightValue.responseInfo;
                    UploadRegionRequestMetrics requestMetrics = singleFlightValue.metrics;
                    JSONObject response = singleFlightValue.response;

                    if (responseInfo != null && responseInfo.isOK() && response != null) {
                        ZonesInfo zonesInfoP = ZonesInfo.createZonesInfo(response);
                        if (zonesInfoP.isValid()) {
                            GlobalCache.getInstance().cache(zonesInfoP, cacheKey);
                            completeHandler.complete(0, responseInfo, requestMetrics);
                        } else {
                            completeHandler.complete(ResponseInfo.ParseError, responseInfo, requestMetrics);
                        }
                    } else {
                        if (responseInfo != null && responseInfo.isNetworkBroken()) {
                            completeHandler.complete(ResponseInfo.NetworkError, responseInfo, requestMetrics);
                        } else {
                            ZonesInfo info = null;
                            if (defaultZone != null) {
                                ZonesInfo infoP = defaultZone.getZonesInfo(token);
                                if (infoP != null && infoP.isValid()) {
                                    infoP.toTemporary();
                                    info = infoP;
                                }
                            }

                            if (info != null) {
                                GlobalCache.getInstance().cache(info, cacheKey);
                                completeHandler.complete(0, responseInfo, requestMetrics);
                            } else {
                                completeHandler.complete(ResponseInfo.ParseError, responseInfo, requestMetrics);
                            }
                        }
                    }
                }
            });

        } catch (Exception e) {
            /// 此处永远不会执行，回调只为占位
            completeHandler.complete(ResponseInfo.NetworkError, ResponseInfo.localIOError(e.toString()), null);
        }
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

    private static class GlobalCache {
        private static GlobalCache globalCache = new GlobalCache();
        private ConcurrentHashMap<String, ZonesInfo> cache = new ConcurrentHashMap<>();

        private static GlobalCache getInstance() {
            return globalCache;
        }

        private synchronized void cache(ZonesInfo zonesInfo, String cacheKey) {
            if (cacheKey == null || cacheKey.isEmpty() || zonesInfo == null) {
                return;
            }
            cache.put(cacheKey, zonesInfo);
        }

        private synchronized ZonesInfo zonesInfoForKey(String cacheKey) {
            if (cacheKey == null || cacheKey.isEmpty()) {
                return null;
            }
            return cache.get(cacheKey);
        }

        private void clearCache() {
            for (ZonesInfo zonesInfo : cache.values()) {
                zonesInfo.toTemporary();
            }
        }
    }
}
