package com.qiniu.android.common;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.request.RequestTransaction;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.utils.SingleFlight;

import org.json.JSONObject;

import java.util.ArrayList;
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
    private String ucServer;
    private Map<String, ZonesInfo> zonesInfoMap = new ConcurrentHashMap<>();
    private ArrayList<RequestTransaction> transactions = new ArrayList<>();

    private static final SingleFlight SingleFlight = new SingleFlight();

    //私有云可能改变ucServer
    public void setUcServer(String ucServer) {
        this.ucServer = ucServer;
    }

    public List<String> getUcServerList() {
        if (ucServer != null) {
            ArrayList<String> serverList = new ArrayList<>();
            serverList.add(ucServer);
            return serverList;
        } else {
            ArrayList<String> serverList = new ArrayList<>();
            serverList.add(Config.preQueryHost00);
            serverList.add(Config.preQueryHost01);
            return serverList;
        }
    }

    @Override
    public ZonesInfo getZonesInfo(UpToken token) {
        if (token == null) {
            return null;
        }
        ZonesInfo zonesInfo = zonesInfoMap.get(token.index());
        return zonesInfo;
    }

    @Override
    public void preQuery(final UpToken token, final QueryHandler completeHandler) {
        if (token == null || !token.isValid()) {
            completeHandler.complete(-1, ResponseInfo.invalidToken("invalid token"), null);
            return;
        }

        final String cacheKey = token.index();
        ZonesInfo zonesInfo = getZonesInfo(token);

        if (zonesInfo == null) {
            zonesInfo = GlobalCache.getInstance().zonesInfoForKey(cacheKey);
            if (zonesInfo != null && zonesInfo.isValid()) {
                zonesInfoMap.put(cacheKey, zonesInfo);
            }
        }

        if (zonesInfo != null && zonesInfo.isValid()) {
            completeHandler.complete(0, ResponseInfo.successResponse(), null);
            return;
        }


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
                    SingleFlightValue singleFlightValue = (SingleFlightValue)value;
                    ResponseInfo responseInfo = singleFlightValue.responseInfo;
                    UploadRegionRequestMetrics requestMetrics = singleFlightValue.metrics;
                    JSONObject response = singleFlightValue.response;

                    if (responseInfo != null && responseInfo.isOK() && response != null) {
                        ZonesInfo zonesInfoP = ZonesInfo.createZonesInfo(response);
                        zonesInfoMap.put(cacheKey, zonesInfoP);
                        GlobalCache.getInstance().cache(response, cacheKey);
                        completeHandler.complete(0, responseInfo, requestMetrics);
                    } else {
                        if (responseInfo.isNetworkBroken()) {
                            completeHandler.complete(ResponseInfo.NetworkError, responseInfo, requestMetrics);
                        } else {
                            ZonesInfo zonesInfoP = FixedZone.localsZoneInfo().getZonesInfo(token);
                            zonesInfoMap.put(cacheKey, zonesInfoP);
                            completeHandler.complete(0, responseInfo, requestMetrics);
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
        private ConcurrentHashMap<String, JSONObject> cache = new ConcurrentHashMap<>();

        private static GlobalCache getInstance() {
            return globalCache;
        }

        private void cache(JSONObject zonesInfo, String cacheKey) {
            if (cacheKey == null || cacheKey.isEmpty()) {
                return;
            }

            if (zonesInfo == null) {
                cache.remove(cacheKey);
            } else {
                cache.put(cacheKey, zonesInfo);
            }
        }

        private ZonesInfo zonesInfoForKey(String cacheKey) {
            if (cacheKey == null || cacheKey.isEmpty()) {
                return null;
            }

            return ZonesInfo.createZonesInfo(cache.get(cacheKey));
        }
    }
}
