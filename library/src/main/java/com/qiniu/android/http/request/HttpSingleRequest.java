package com.qiniu.android.http.request;

import com.qiniu.android.collect.ReportItem;
import com.qiniu.android.collect.UploadInfoReporter;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.connectCheck.ConnectChecker;
import com.qiniu.android.http.dns.DnsSource;
import com.qiniu.android.http.dns.DnsPrefetcher;
import com.qiniu.android.http.networkStatus.NetworkStatusManager;
import com.qiniu.android.http.request.httpclient.SystemHttpClient;
import com.qiniu.android.http.request.handler.CheckCancelHandler;
import com.qiniu.android.http.request.handler.RequestProgressHandler;
import com.qiniu.android.http.request.handler.RequestShouldRetryHandler;
import com.qiniu.android.http.metrics.UploadSingleRequestMetrics;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.GlobalConfiguration;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.utils.LogUtil;
import com.qiniu.android.utils.StringUtils;
import com.qiniu.android.utils.Utils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;


class HttpSingleRequest {

    private int currentRetryTime;
    private final Configuration config;
    private final UploadOptions uploadOption;
    private final UpToken token;
    private final UploadRequestInfo requestInfo;
    private final UploadRequestState requestState;

    private ArrayList<UploadSingleRequestMetrics> requestMetricsList;

    private IRequestClient client;

    HttpSingleRequest(Configuration config,
                      UploadOptions uploadOption,
                      UpToken token,
                      UploadRequestInfo requestInfo,
                      UploadRequestState requestState) {
        this.config = config;
        this.uploadOption = uploadOption;
        this.token = token;
        this.requestInfo = requestInfo;
        this.requestState = requestState;
        this.currentRetryTime = 0;
    }

    void request(Request request,
                 IUploadServer server,
                 boolean isAsync,
                 RequestShouldRetryHandler shouldRetryHandler,
                 RequestProgressHandler progressHandler,
                 RequestCompleteHandler completeHandler) {
        currentRetryTime = 0;
        requestMetricsList = new ArrayList<>();
        retryRequest(request, server, isAsync, shouldRetryHandler, progressHandler, completeHandler);
    }

    private void retryRequest(final Request request,
                              final IUploadServer server,
                              final boolean isAsync,
                              final RequestShouldRetryHandler shouldRetryHandler,
                              final RequestProgressHandler progressHandler,
                              final RequestCompleteHandler completeHandler) {

        if (server.isHttp3()) {
            client = new SystemHttpClient();
        } else {
            client = new SystemHttpClient();
        }

        final CheckCancelHandler checkCancelHandler = new CheckCancelHandler() {
            @Override
            public boolean checkCancel() {
                boolean isCancelled = requestState.isUserCancel();
                if (!isCancelled && uploadOption.cancellationSignal != null) {
                    isCancelled = uploadOption.cancellationSignal.isCancelled();
                }
                return isCancelled;
            }
        };

        LogUtil.i("key:" + StringUtils.toNonnullString(requestInfo.key) +
                " retry:" + currentRetryTime +
                " url:" + StringUtils.toNonnullString(request.urlString) +
                " ip:" + StringUtils.toNonnullString(request.ip));

        client.request(request, isAsync, config.proxy, new IRequestClient.RequestClientProgress() {
            @Override
            public void progress(long totalBytesWritten, long totalBytesExpectedToWrite) {
                if (checkCancelHandler.checkCancel()) {
                    requestState.setUserCancel(true);
                    if (client != null) {
                        client.cancel();
                    }
                } else if (progressHandler != null) {
                    progressHandler.progress(totalBytesWritten, totalBytesExpectedToWrite);
                }
            }
        }, new IRequestClient.RequestClientCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadSingleRequestMetrics metrics, JSONObject response) {
                if (metrics != null) {
                    requestMetricsList.add(metrics);
                }

                if (checkCancelHandler.checkCancel()) {
                    responseInfo = ResponseInfo.cancelled();
                    reportRequest(responseInfo, server, metrics);
                    completeAction(server, responseInfo, responseInfo.response, metrics, completeHandler);
                    return;
                }
                
                if (responseInfo != null) {
                    responseInfo = responseInfo.checkMaliciousResponse();
                }

                boolean isSafeDnsSource = DnsSource.isCustom(server.getSource()) || DnsSource.isDoh(server.getSource()) || DnsSource.isDnspod(server.getSource());
                boolean hijacked = responseInfo != null && responseInfo.isNotQiniu() && !isSafeDnsSource;
                if (hijacked && metrics != null) {
                    metrics.hijacked = UploadSingleRequestMetrics.RequestHijacked;
                    try {
                        metrics.syncDnsSource = DnsPrefetcher.getInstance().lookupBySafeDns(server.getHost());
                    } catch (Exception e) {
                        metrics.syncDnsError = e.toString();
                    }
                }

                if (!hijacked && shouldCheckConnect(responseInfo)) {
                    UploadSingleRequestMetrics checkMetrics = ConnectChecker.check();
                    if (metrics != null) {
                        metrics.connectCheckMetrics = checkMetrics;
                    }
                    if (!ConnectChecker.isConnected(checkMetrics)) {
                        String message = responseInfo == null ? "" : ("check origin statusCode:" + responseInfo.statusCode + " error:" + responseInfo.error);
                        responseInfo = ResponseInfo.errorInfo(ResponseInfo.NetworkSlow, message);
                    } else if (metrics != null && !isSafeDnsSource) {
                        metrics.hijacked = UploadSingleRequestMetrics.RequestMaybeHijacked;
                        try {
                            metrics.syncDnsSource = DnsPrefetcher.getInstance().lookupBySafeDns(server.getHost());
                        } catch (Exception e) {
                            metrics.syncDnsError = e.toString();
                        }
                    }
                }

                reportRequest(responseInfo, server, metrics);

                LogUtil.i("key:" + StringUtils.toNonnullString(requestInfo.key) +
                        " response:" + StringUtils.toNonnullString(responseInfo));
                if (shouldRetryHandler != null && shouldRetryHandler.shouldRetry(responseInfo, response)
                        && currentRetryTime < config.retryMax
                        && (responseInfo != null && responseInfo.couldHostRetry())) {
                    currentRetryTime += 1;

                    try {
                        Thread.sleep(config.retryInterval);
                    } catch (InterruptedException ignored) {
                    }
                    retryRequest(request, server, isAsync, shouldRetryHandler, progressHandler, completeHandler);
                } else {
                    completeAction(server, responseInfo, response, metrics, completeHandler);
                }
            }
        });

    }

    private boolean shouldCheckConnect(ResponseInfo responseInfo) {
        if (!GlobalConfiguration.getInstance().connectCheckEnable) {
            return false;
        }

        return responseInfo != null &&
                (responseInfo.statusCode == ResponseInfo.NetworkError || /* network error */
                        responseInfo.statusCode == -1001 || /* timeout */
                        responseInfo.statusCode == -1003 || /* unknown host */
                        responseInfo.statusCode == -1004 || /* cannot connect to host */
                        responseInfo.statusCode == -1005 || /* connection lost */
                        responseInfo.statusCode == -1009 || /* not connected to host */
                        responseInfo.isTlsError());
    }

    private synchronized void completeAction(IUploadServer server,
                                             ResponseInfo responseInfo,
                                             JSONObject response,
                                             UploadSingleRequestMetrics requestMetrics,
                                             RequestCompleteHandler completeHandler) {

        if (client == null) {
            return;
        }
        client = null;

        updateHostNetworkStatus(responseInfo, server, requestMetrics);

        if (completeHandler != null) {
            completeHandler.complete(responseInfo, requestMetricsList, response);
        }
    }

    private void updateHostNetworkStatus(ResponseInfo responseInfo, IUploadServer server, UploadSingleRequestMetrics requestMetrics) {
        if (requestMetrics == null) {
            return;
        }
        long byteCount = requestMetrics.bytesSend();
        long second = requestMetrics.totalElapsedTime();
        if (second > 0 && byteCount >= 1024 * 1024) {
            int speed = (int) (byteCount * 1000 / second);
            String type = NetworkStatusManager.getNetworkStatusType(server.getHost(), server.getIp());
            NetworkStatusManager.getInstance().updateNetworkStatus(type, speed);
        }
    }

    private void reportRequest(ResponseInfo responseInfo,
                               IUploadServer server,
                               UploadSingleRequestMetrics requestMetrics) {

        if (token == null || !token.isValid() || requestInfo == null || !requestInfo.shouldReportRequestLog() || requestMetrics == null) {
            return;
        }

        long currentTimestamp = Utils.currentTimestamp();
        ReportItem item = new ReportItem();
        item.setReport(ReportItem.LogTypeRequest, ReportItem.RequestKeyLogType);
        item.setReport((requestMetrics.getStartDate().getTime() / 1000), ReportItem.RequestKeyUpTime);
        item.setReport(ReportItem.requestReportStatusCode(responseInfo), ReportItem.RequestKeyStatusCode);
        item.setReport(responseInfo != null ? responseInfo.reqId : null, ReportItem.RequestKeyRequestId);
        item.setReport(requestMetrics.request != null ? requestMetrics.request.host : null, ReportItem.RequestKeyHost);
        item.setReport(requestMetrics.remoteAddress, ReportItem.RequestKeyRemoteIp);
        item.setReport(requestMetrics.remotePort, ReportItem.RequestKeyPort);
        item.setReport(requestInfo.bucket, ReportItem.RequestKeyTargetBucket);
        item.setReport(requestInfo.key, ReportItem.RequestKeyTargetKey);
        item.setReport(requestMetrics.totalElapsedTime(), ReportItem.RequestKeyTotalElapsedTime);
        item.setReport(requestMetrics.totalDnsTime(), ReportItem.RequestKeyDnsElapsedTime);
        item.setReport(requestMetrics.totalConnectTime(), ReportItem.RequestKeyConnectElapsedTime);
        item.setReport(requestMetrics.totalSecureConnectTime(), ReportItem.RequestKeyTLSConnectElapsedTime);
        item.setReport(requestMetrics.totalRequestTime(), ReportItem.RequestKeyRequestElapsedTime);
        item.setReport(requestMetrics.totalWaitTime(), ReportItem.RequestKeyWaitElapsedTime);
        item.setReport(requestMetrics.totalWaitTime(), ReportItem.RequestKeyResponseElapsedTime);
        item.setReport(requestMetrics.totalResponseTime(), ReportItem.RequestKeyResponseElapsedTime);
        item.setReport(requestInfo.fileOffset, ReportItem.RequestKeyFileOffset);
        item.setReport(requestMetrics.bytesSend(), ReportItem.RequestKeyBytesSent);
        item.setReport(requestMetrics.totalBytes(), ReportItem.RequestKeyBytesTotal);
        item.setReport(Utils.getCurrentProcessID(), ReportItem.RequestKeyPid);
        item.setReport(Utils.getCurrentThreadID(), ReportItem.RequestKeyTid);
        item.setReport(requestInfo.targetRegionId, ReportItem.RequestKeyTargetRegionId);
        item.setReport(requestInfo.currentRegionId, ReportItem.RequestKeyCurrentRegionId);
        String errorType = ReportItem.requestReportErrorType(responseInfo);
        item.setReport(errorType, ReportItem.RequestKeyErrorType);
        String errorDesc = null;
        if (responseInfo != null && errorType != null) {
            errorDesc = responseInfo.error != null ? responseInfo.error : responseInfo.message;
        }
        item.setReport(errorDesc, ReportItem.RequestKeyErrorDescription);
        item.setReport(requestInfo.requestType, ReportItem.RequestKeyUpType);
        item.setReport(Utils.systemName(), ReportItem.RequestKeyOsName);
        item.setReport(Utils.systemVersion(), ReportItem.RequestKeyOsVersion);
        item.setReport(Utils.sdkLanguage(), ReportItem.RequestKeySDKName);
        item.setReport(Utils.sdkVerion(), ReportItem.RequestKeySDKVersion);
        item.setReport(currentTimestamp, ReportItem.RequestKeyClientTime);
        item.setReport(Utils.getCurrentNetworkType(), ReportItem.RequestKeyNetworkType);
        item.setReport(Utils.getCurrentSignalStrength(), ReportItem.RequestKeySignalStrength);

        item.setReport(server.getSource(), ReportItem.RequestKeyPrefetchedDnsSource);
        if (server.getIpPrefetchedTime() != null) {
            Long prefetchTime = currentTimestamp / 1000 - server.getIpPrefetchedTime();
            item.setReport(prefetchTime, ReportItem.RequestKeyPrefetchedBefore);
        }
        item.setReport(DnsPrefetcher.getInstance().lastPrefetchErrorMessage, ReportItem.RequestKeyPrefetchedErrorMessage);

        item.setReport(requestMetrics.clientName, ReportItem.RequestKeyHttpClient);
        item.setReport(requestMetrics.clientVersion, ReportItem.RequestKeyHttpClientVersion);

        if (!GlobalConfiguration.getInstance().connectCheckEnable) {
            item.setReport("disable", ReportItem.RequestKeyNetworkMeasuring);
        } else if (requestMetrics.connectCheckMetrics != null) {
            String connectCheckDuration = String.format(Locale.ENGLISH, "%d", requestMetrics.connectCheckMetrics.totalElapsedTime());
            String connectCheckStatusCode = "";
            if (requestMetrics.connectCheckMetrics.response != null) {
                connectCheckStatusCode = String.format(Locale.ENGLISH, "%d", requestMetrics.connectCheckMetrics.response.statusCode);
            }
            String networkMeasuring = String.format("duration:%s status_code:%s", connectCheckDuration, connectCheckStatusCode);
            item.setReport(networkMeasuring, ReportItem.RequestKeyNetworkMeasuring);
        }

        // 劫持标记
        item.setReport(requestMetrics.hijacked, ReportItem.RequestKeyHijacking);
        item.setReport(requestMetrics.syncDnsSource, ReportItem.RequestKeyDnsSource);
        item.setReport(requestMetrics.syncDnsError, ReportItem.RequestKeyDnsErrorMessage);

        // 统计当前请求上传速度 / 总耗时
        if (responseInfo.isOK()) {
            item.setReport(requestMetrics.perceptiveSpeed(), ReportItem.RequestKeyPerceptiveSpeed);
        }

        item.setReport(requestMetrics.httpVersion, ReportItem.RequestKeyHttpVersion);

        UploadInfoReporter.getInstance().report(item, token.token);
    }

    interface RequestCompleteHandler {
        void complete(ResponseInfo responseInfo,
                      ArrayList<UploadSingleRequestMetrics> requestMetricsList,
                      JSONObject response);
    }
}

