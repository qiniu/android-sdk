package com.qiniu.android.common;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.request.RequestTransaction;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.storage.UpToken;

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
    private Map<String, ZonesInfo> ZonesInfoMap = new ConcurrentHashMap<>();
    private ArrayList<RequestTransaction> transactions = new ArrayList<>();

    //私有云可能改变ucServer
    public void setUcServer(String ucServer) {
        this.ucServer = ucServer;
    }

    public List<String> getUcServerList() {
        if (ucServer != null){
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
        ZonesInfo zonesInfo = ZonesInfoMap.get(token.index());
        return zonesInfo;
    }

    @Override
    public void preQuery(final UpToken token, final QueryHandler completeHandler) {
        final ZonesInfo zonesInfo = getZonesInfo(token);

        if (zonesInfo != null) {
            completeHandler.complete(0, null, null);
            return;
        }
        final RequestTransaction transaction = createUploadRequestTransaction(token);
        transaction.queryUploadHosts(true, new RequestTransaction.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                if (responseInfo != null && responseInfo.isOK() && response != null){
                    ZonesInfo zonesInfoP = ZonesInfo.createZonesInfo(response);
                    ZonesInfoMap.put(token.index(), zonesInfoP);
                    completeHandler.complete(0, responseInfo, requestMetrics);
                } else {
                    if (responseInfo.isNetworkBroken()){
                        completeHandler.complete(ResponseInfo.NetworkError, responseInfo, requestMetrics);
                    } else {
                        ZonesInfo zonesInfoP = FixedZone.localsZoneInfo().getZonesInfo(token);
                        ZonesInfoMap.put(token.index(), zonesInfoP);
                        completeHandler.complete(0, responseInfo, requestMetrics);
                    }
                }
                destroyUploadRequestTransaction(transaction);
            }
        });

    }


    private RequestTransaction createUploadRequestTransaction(UpToken token){
        List<String> hosts = getUcServerList();

        RequestTransaction transaction = new RequestTransaction(hosts, ZoneInfo.EmptyRegionId, token);
        transactions.add(transaction);
        return transaction;
    }

    private void destroyUploadRequestTransaction(RequestTransaction transaction){
        transactions.remove(transaction);
    }
}
