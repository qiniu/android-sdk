package com.qiniu.android.http.request;


import android.util.Log;

import com.qiniu.android.collect.ReportItem;
import com.qiniu.android.collect.UploadInfoReporter;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.request.httpclient.SystemHttpClient;
import com.qiniu.android.http.request.handler.CheckCancelHandler;
import com.qiniu.android.http.request.handler.RequestProgressHandler;
import com.qiniu.android.http.request.handler.RequestShouldRetryHandler;
import com.qiniu.android.http.metrics.UploadSingleRequestMetrics;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.utils.AsyncRun;
import com.qiniu.android.utils.Utils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.TimerTask;

public class HttpSingleRequest {

    private int currentRetryTime;
    private final Configuration config;
    private final UploadOptions uploadOption;
    private final UpToken token;
    private final UploadRequestInfo requestInfo;
    private final UploadRequstState requstState;

    private ArrayList <UploadSingleRequestMetrics> requestMetricsList;

    private RequestClient client;


    public HttpSingleRequest(Configuration config,
                             UploadOptions uploadOption,
                             UpToken token,
                             UploadRequestInfo requestInfo,
                             UploadRequstState requstState) {
        this.config = config;
        this.uploadOption = uploadOption;
        this.token = token;
        this.requestInfo = requestInfo;
        this.requstState = requstState;
        currentRetryTime = 1;
    }

    public void request(Request request,
                        boolean isAsyn,
                        boolean isSkipDns,
                        RequestShouldRetryHandler shouldRetryHandler,
                        RequestProgressHandler progressHandler,
                        RequestCompleteHandler completeHandler){
        currentRetryTime = 1;
        requestMetricsList = new ArrayList<UploadSingleRequestMetrics>();
        retryRequest(request, isAsyn, isSkipDns, shouldRetryHandler, progressHandler, completeHandler);
    }

    private void retryRequest(final Request request,
                              final boolean isAsyn,
                              final boolean isSkipDns,
                              final RequestShouldRetryHandler shouldRetryHandler,
                              final RequestProgressHandler progressHandler,
                              final RequestCompleteHandler completeHandler){
        if (isSkipDns){
            client = new SystemHttpClient();
        } else {
            client = new SystemHttpClient();
        }

        final CheckCancelHandler checkCancelHandler = new CheckCancelHandler() {
            @Override
            public boolean checkCancel() {
                boolean isCancel = requstState.getIsUserCancel();
                if (isCancel == false && uploadOption.cancellationSignal != null) {
                    isCancel = uploadOption.cancellationSignal.isCancelled();
                }
                return isCancel;
            }
        };

        Log.w("SingleRequest", ("== request host:" + request.host + " ip:" + request.ip));
        client.request(request, isAsyn, config.proxy, new RequestClient.RequestClientProgress() {
            @Override
            public void progress(long totalBytesWritten, long totalBytesExpectedToWrite) {
                if (checkCancelHandler.checkCancel() == true) {
                    requstState.setUserCancel(true);
                    client.cancel();
                } else if (progressHandler != null){
                    progressHandler.progress(totalBytesWritten, totalBytesExpectedToWrite);
                }
            }
        }, new RequestClient.RequestClientCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadSingleRequestMetrics metrics, JSONObject response) {
                if (metrics != null){
                    requestMetricsList.add(metrics);
                }
                if (shouldRetryHandler != null && shouldRetryHandler.shouldRetry(responseInfo, response)
                    && currentRetryTime < config.retryMax
                    && responseInfo.couldHostRetry()){
                    currentRetryTime += 1;

                    if (isAsyn) {
                        AsyncRun.runInBack(config.retryInterval, new Runnable() {
                            @Override
                            public void run() {
                                retryRequest(request, isAsyn, isSkipDns, shouldRetryHandler, progressHandler, completeHandler);
                            }
                        });
                    } else {
                        try {
                            Thread.sleep(config.retryInterval);
                        } catch (InterruptedException ignored) {
                        } finally {
                            retryRequest(request, isAsyn, isSkipDns, shouldRetryHandler, progressHandler, completeHandler);
                        }
                    }
                } else {
                    completeAction(responseInfo, response, metrics, completeHandler);
                }
            }
        });

    }


    private void completeAction(ResponseInfo responseInfo,
                                JSONObject response,
                                UploadSingleRequestMetrics requestMetrics,
                                RequestCompleteHandler completeHandler) {

        reportRequest(responseInfo, requestMetrics);
        if (completeHandler != null){
            completeHandler.complete(responseInfo, requestMetricsList, response);
        }
    }

    private void reportRequest(ResponseInfo responseInfo,
                               UploadSingleRequestMetrics requestMetrics){

        UploadSingleRequestMetrics requestMetricsP = requestMetrics != null ? requestMetrics : new UploadSingleRequestMetrics();

        ReportItem item = new ReportItem();
        item.setReport(ReportItem.LogTypeRequest, ReportItem.RequestKeyLogType);
        item.setReport(ReportItem.requestReportStatusCode(responseInfo), ReportItem.RequestKeyStatusCode);
        item.setReport(responseInfo.reqId, ReportItem.RequestKeyRequestId);
        item.setReport(requestMetricsP.request.host(), ReportItem.RequestKeyHost);
        item.setReport(requestMetricsP.remoteAddress, ReportItem.RequestKeyRemoteIp);
        item.setReport(requestMetricsP.localPort, ReportItem.RequestKeyPort);
        item.setReport(requestInfo.bucket, ReportItem.RequestKeyTargetBucket);
        item.setReport(requestInfo.key, ReportItem.RequestKeyTargetKey);
        item.setReport(requestMetricsP.totalElaspsedTime(), ReportItem.RequestKeyTotalElapsedTime);
        item.setReport(requestMetricsP.totalDnsTime(), ReportItem.RequestKeyDnsElapsedTime);
        item.setReport(requestMetricsP.totalConnectTime(), ReportItem.RequestKeyConnectElapsedTime);
        item.setReport(requestMetricsP.totalSecureConnectTime(), ReportItem.RequestKeyTLSConnectElapsedTime);
        item.setReport(requestMetricsP.totalRequestTime(), ReportItem.RequestKeyRequestElapsedTime);
        item.setReport(requestMetricsP.totalWaitTime(), ReportItem.RequestKeyWaitElapsedTime);
        item.setReport(requestMetricsP.totalWaitTime(), ReportItem.RequestKeyResponseElapsedTime);
        item.setReport(requestMetricsP.totalResponseTime(), ReportItem.RequestKeyResponseElapsedTime);
        item.setReport(requestInfo.fileOffset, ReportItem.RequestKeyFileOffset);
        item.setReport(requestMetricsP.bytesSend(), ReportItem.RequestKeyBytesSent);
        item.setReport(requestMetricsP.totalBytes(), ReportItem.RequestKeyBytesTotal);
        item.setReport(Utils.getCurrentProcessID(), ReportItem.RequestKeyPid);
        item.setReport(Utils.getCurrentThreadID(), ReportItem.RequestKeyTid);
        item.setReport(requestInfo.targetRegionId, ReportItem.RequestKeyTargetRegionId);
        item.setReport(requestInfo.currentRegionId, ReportItem.RequestKeyCurrentRegionId);
        item.setReport(ReportItem.requestReportErrorType(responseInfo), ReportItem.RequestKeyErrorType);
        String errorDesc = ReportItem.requestReportErrorType(responseInfo) != null ? responseInfo.message : null;
        item.setReport(errorDesc, ReportItem.RequestKeyErrorDescription);
        item.setReport(requestInfo.requestType, ReportItem.RequestKeyUpType);
        item.setReport(Utils.systemName(), ReportItem.RequestKeyOsName);
        item.setReport(Utils.systemVersion(), ReportItem.RequestKeyOsVersion);
        item.setReport(Utils.sdkLanguage(), ReportItem.RequestKeySDKName);
        item.setReport(Utils.sdkVerion(), ReportItem.RequestKeySDKVersion);
        item.setReport(Utils.currentTimestamp(), ReportItem.RequestKeyClientTime);
        item.setReport(Utils.getCurrentNetworkType(), ReportItem.RequestKeyNetworkType);
        item.setReport(Utils.getCurrentSignalStrength(), ReportItem.RequestKeySignalStrength);

        UploadInfoReporter.getInstance().report(item, token.token);
    }

    public interface RequestCompleteHandler {
        public void complete(ResponseInfo responseInfo,
                             ArrayList<UploadSingleRequestMetrics> requestMetricsList,
                             JSONObject response);
    }
}

