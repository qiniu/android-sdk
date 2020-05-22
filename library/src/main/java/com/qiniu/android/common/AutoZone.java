package com.qiniu.android.common;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.request.RequestTranscation;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.storage.UpToken;

import org.json.JSONObject;

import java.util.ArrayList;
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
    private ArrayList<RequestTranscation> transcations = new ArrayList<>();

    //私有云可能改变ucServer
    public void setUcServer(String ucServer) {
        this.ucServer = ucServer;
    }

    public String getUcServer() {
        if (ucServer != null){
            return ucServer;
        } else {
            return "uc.qbox.me";
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
            completeHandler.complete(0, null);
            return;
        }
        final RequestTranscation transcation = createUploadRequestTranscation(token);
        transcation.quertUploadHosts(true, new RequestTranscation.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                if (responseInfo != null && responseInfo.isOK() && response != null){
                    ZonesInfo zonesInfoP = ZonesInfo.createZonesInfo(response);
                    ZonesInfoMap.put(token.index(), zonesInfoP);
                    completeHandler.complete(0, responseInfo);
                } else {
                    completeHandler.complete(ResponseInfo.NetworkError, responseInfo);
                }
                destoryUploadRequestTranscation(transcation);
            }
        });

    }


    private RequestTranscation createUploadRequestTranscation(UpToken token){
        ArrayList<String> hosts = new ArrayList<>();
        hosts.add(getUcServer());
        RequestTranscation transcation = new RequestTranscation(hosts, token);
        transcations.add(transcation);
        return transcation;
    }

    private void destoryUploadRequestTranscation(RequestTranscation transcation){
        transcations.remove(transcation);
    }
}
