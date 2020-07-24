package com.qiniu.android.http.request;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.request.handler.RequestProgressHandler;
import com.qiniu.android.http.request.handler.RequestShouldRetryHandler;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.http.metrics.UploadSingleRequestMetrics;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.storage.UploadOptions;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

class HttpRegionRequest {

    private final Configuration config;
    private final UploadOptions uploadOption;
    private final UpToken token;
    private final IUploadRegion region;
    private final UploadRequestInfo requestInfo;

    private boolean isUseOldServer;
    private HttpSingleRequest singleRequest;
    private IUploadServer currentServer;
    private UploadRegionRequestMetrics requestMetrics;

    HttpRegionRequest(Configuration config,
                      UploadOptions uploadOption,
                      UpToken token,
                      IUploadRegion region,
                      UploadRequestInfo requestInfo,
                      UploadRequestState requestState) {
        this.config = config;
        this.uploadOption = uploadOption;
        this.token = token;
        this.region = region;
        this.requestInfo = requestInfo;

        singleRequest = new HttpSingleRequest(config, uploadOption, token, requestInfo, requestState);
    }

    void get(String action,
             boolean isAsync,
             Map<String, String>header,
             RequestShouldRetryHandler shouldRetryHandler,
             RequestCompleteHandler completeHandler){
        requestMetrics = new UploadRegionRequestMetrics(region);
        performRequest(getNextServer(null), action, isAsync, null, header, "GET", shouldRetryHandler, null, completeHandler);
    }

    void post(String action,
              boolean isAsync,
              byte[] data,
              Map<String, String>header,
              RequestShouldRetryHandler shouldRetryHandler,
              RequestProgressHandler progressHandler,
              RequestCompleteHandler completeHandler){
        requestMetrics = new UploadRegionRequestMetrics(region);
        performRequest(getNextServer(null), action, isAsync, data, header, "POST", shouldRetryHandler, progressHandler, completeHandler);
    }

    private void performRequest(IUploadServer server,
                                final String action,
                                final boolean isAsync,
                                final byte[] data,
                                final Map<String, String>header,
                                final String method,
                                final RequestShouldRetryHandler shouldRetryHandler,
                                final RequestProgressHandler progressHandler,
                                final RequestCompleteHandler completeHandler){

        if (server == null || server.getHost() == null || server.getHost().length() == 0) {
            ResponseInfo responseInfo = ResponseInfo.invalidArgument("server error");
            completeAction(responseInfo, null, completeHandler);
            return;
        }

        String serverHost = server.getHost();
        String serverIP = server.getIp();

        if (config.urlConverter != null){
            serverHost = config.urlConverter.convert(serverHost);
            serverIP = null;
        }

        currentServer = server;

        boolean toSkipDns;
        String scheme = config.useHttps ? "https://" : "http://";
        String urlString = scheme + serverHost + (action != null ? action : "");
        if (serverIP != null && serverIP.length() > 0) {
            toSkipDns = false;
        } else {
            toSkipDns = true;
        }
        Request request = new Request(urlString, method, header, data, config.connectTimeout);
        request.host = serverHost;
        request.ip = serverIP;
        request.uploadServer = server;
        singleRequest.request(request, isAsync, toSkipDns, shouldRetryHandler, progressHandler, new HttpSingleRequest.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, ArrayList<UploadSingleRequestMetrics> requestMetricsList, JSONObject response) {

                requestMetrics.addMetricsList(requestMetricsList);

                if (shouldRetryHandler.shouldRetry(responseInfo, response)
                        && config.allowBackupHost
                        && responseInfo.couldRegionRetry()){

                    IUploadServer newServer = getNextServer(responseInfo);
                    if (newServer != null){
                        performRequest(newServer, action, isAsync, data, header, method, shouldRetryHandler, progressHandler, completeHandler);
                    } else {
                        completeAction(responseInfo, response, completeHandler);
                    }
                } else {
                    completeAction(responseInfo, response, completeHandler);
                }
            }
        });

    }


    private void completeAction(ResponseInfo responseInfo,
                                JSONObject response,
                                RequestCompleteHandler completeHandler){

        if (completeHandler != null){
            completeHandler.complete(responseInfo, requestMetrics, response);
        }
    }

    private IUploadServer getNextServer(ResponseInfo responseInfo){
        if (responseInfo == null) {
            return region.getNextServer(false, null);
        }

        if (responseInfo.isTlsError()) {
            isUseOldServer = true;
        }
        return region.getNextServer(isUseOldServer, currentServer);
    }


    interface RequestCompleteHandler {
        void complete(ResponseInfo responseInfo,
                      UploadRegionRequestMetrics requestMetrics,
                      JSONObject response);
    }
}
