package com.qiniu.android.http.request;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.dns.DnsSource;
import com.qiniu.android.http.request.handler.RequestProgressHandler;
import com.qiniu.android.http.request.handler.RequestShouldRetryHandler;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.http.metrics.UploadSingleRequestMetrics;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.utils.LogUtil;
import com.qiniu.android.utils.StringUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

class HttpRegionRequest {

    private final Configuration config;
    private final UploadOptions uploadOption;
    private final UpToken token;
    private final IUploadRegion region;
    private final UploadRequestInfo requestInfo;

    private UploadRequestState requestState;
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
        this.requestState = requestState;

        singleRequest = new HttpSingleRequest(config, uploadOption, token, requestInfo, requestState);
    }

    void get(String action,
             boolean isAsync,
             Map<String, String> header,
             RequestShouldRetryHandler shouldRetryHandler,
             RequestCompleteHandler completeHandler) {
        requestMetrics = new UploadRegionRequestMetrics(region);
        requestMetrics.start();
        performRequest(getNextServer(null), action, isAsync, null, header, "GET", shouldRetryHandler, null, completeHandler);
    }

    void post(String action,
              boolean isAsync,
              byte[] data,
              Map<String, String> header,
              RequestShouldRetryHandler shouldRetryHandler,
              RequestProgressHandler progressHandler,
              RequestCompleteHandler completeHandler) {
        requestMetrics = new UploadRegionRequestMetrics(region);
        requestMetrics.start();
        performRequest(getNextServer(null), action, isAsync, data, header, "POST", shouldRetryHandler, progressHandler, completeHandler);
    }

    void put(String action,
             boolean isAsync,
             byte[] data,
             Map<String, String> header,
             RequestShouldRetryHandler shouldRetryHandler,
             RequestProgressHandler progressHandler,
             RequestCompleteHandler completeHandler) {
        requestMetrics = new UploadRegionRequestMetrics(region);
        requestMetrics.start();
        performRequest(getNextServer(null), action, isAsync, data, header, "PUT", shouldRetryHandler, progressHandler, completeHandler);
    }

    private void performRequest(final IUploadServer server,
                                final String action,
                                final boolean isAsync,
                                final byte[] data,
                                final Map<String, String> header,
                                final String method,
                                final RequestShouldRetryHandler shouldRetryHandler,
                                final RequestProgressHandler progressHandler,
                                final RequestCompleteHandler completeHandler) {

        if (server == null || server.getHost() == null || server.getHost().length() == 0) {
            ResponseInfo responseInfo = ResponseInfo.sdkInteriorError("server error");
            completeAction(responseInfo, null, completeHandler);
            return;
        }

        currentServer = server;

        String serverHost = server.getHost();
        String serverIP = server.getIp();

        if (config.urlConverter != null) {
            serverIP = null;
            serverHost = config.urlConverter.convert(serverHost);
        }

        String scheme = config.useHttps ? "https://" : "http://";
        String urlString = scheme + serverHost + (action != null ? action : "");
        final Request request = new Request(urlString, method, header, data,
                config.connectTimeout,
                config.writeTimeout,
                config.responseTimeout);
        request.setHost(serverHost);
        LogUtil.i("key:" + StringUtils.toNonnullString(requestInfo.key) +
                " url:" + StringUtils.toNonnullString(request.urlString));
        LogUtil.i("key:" + StringUtils.toNonnullString(requestInfo.key) +
                " headers:" + StringUtils.toNonnullString(request.allHeaders));

        singleRequest.request(request, server, isAsync, shouldRetryHandler, progressHandler, new HttpSingleRequest.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, ArrayList<UploadSingleRequestMetrics> requestMetricsList, JSONObject response) {

                requestMetrics.addMetricsList(requestMetricsList);

                boolean hijackedAndNeedRetry = false;
                if (requestMetricsList != null && requestMetricsList.size() > 0) {
                    UploadSingleRequestMetrics metrics = requestMetricsList.get(requestMetricsList.size() - 1);
                    boolean isSafeDnsSource = DnsSource.isCustom(metrics.getSyncDnsSource()) || DnsSource.isDoh(metrics.getSyncDnsSource()) || DnsSource.isDnspod(metrics.getSyncDnsSource());
                    if ((metrics.isForsureHijacked() || metrics.isMaybeHijacked() && isSafeDnsSource)) {
                        hijackedAndNeedRetry = true;
                    }
                }

                if (hijackedAndNeedRetry) {
                    region.updateIpListFormHost(server.getHost());
                }

                if ((shouldRetryHandler.shouldRetry(responseInfo, response)
                        && config.allowBackupHost
                        && responseInfo.couldRegionRetry()) || hijackedAndNeedRetry) {

                    IUploadServer newServer = getNextServer(responseInfo);
                    if (newServer != null) {
                        performRequest(newServer, action, isAsync, request.httpBody, header, method, shouldRetryHandler, progressHandler, completeHandler);
                        request.httpBody = null;
                    } else {
                        request.httpBody = null;
                        completeAction(responseInfo, response, completeHandler);
                    }
                } else {
                    request.httpBody = null;
                    completeAction(responseInfo, response, completeHandler);
                }
            }
        });

    }


    private void completeAction(ResponseInfo responseInfo,
                                JSONObject response,
                                RequestCompleteHandler completeHandler) {

        requestMetrics.end();
        singleRequest = null;
        if (completeHandler != null) {
            completeHandler.complete(responseInfo, requestMetrics, response);
        }
    }

    private IUploadServer getNextServer(ResponseInfo responseInfo) {

        if (requestState != null && responseInfo != null && responseInfo.isTlsError()) {
            requestState.setUseOldServer(true);
        }

        return region.getNextServer(requestState, responseInfo, currentServer);
    }


    interface RequestCompleteHandler {
        void complete(ResponseInfo responseInfo,
                      UploadRegionRequestMetrics requestMetrics,
                      JSONObject response);
    }
}
