package com.qiniu.android.http.newHttp;


import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.newHttp.HttpClient.SystemHttpClient;
import com.qiniu.android.http.newHttp.handler.CheckCancelHandler;
import com.qiniu.android.http.newHttp.handler.RequestProgressHandler;
import com.qiniu.android.http.newHttp.handler.RequestShouldRetryHandler;
import com.qiniu.android.http.newHttp.metrics.UploadSingleRequestMetrics;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.storage.UploadOptions;

import org.json.JSONObject;

import java.util.ArrayList;

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
        currentRetryTime = 0;
    }

    public void request(Request request,
                        boolean isAsyn,
                        boolean isSkipDns,
                        RequestShouldRetryHandler shouldRetryHandler,
                        RequestProgressHandler progressHandler,
                        RequestCompleteHandler completeHandler){
        currentRetryTime = 0;
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

                    retryRequest(request, isAsyn, isSkipDns, shouldRetryHandler, progressHandler, completeHandler);

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

//        QNReportItem *item = [QNReportItem item];
//        [item setReportValue:QNReportLogTypeRequest forKey:QNReportRequestKeyLogType];
//        [item setReportValue:info.requestReportStatusCode forKey:QNReportRequestKeyStatusCode];
//        [item setReportValue:info.reqId forKey:QNReportRequestKeyRequestId];
//        [item setReportValue:taskMetricsP.request.qn_domain forKey:QNReportRequestKeyHost];
//        [item setReportValue:taskMetricsP.remoteAddress forKey:QNReportRequestKeyRemoteIp];
//        [item setReportValue:taskMetricsP.localPort forKey:QNReportRequestKeyPort];
//        [item setReportValue:self.requestInfo.bucket forKey:QNReportRequestKeyTargetBucket];
//        [item setReportValue:self.requestInfo.key forKey:QNReportRequestKeyTargetKey];
//        [item setReportValue:taskMetricsP.totalElaspsedTime forKey:QNReportRequestKeyTotalElaspsedTime];
//        [item setReportValue:taskMetricsP.totalDnsTime forKey:QNReportRequestKeyDnsElapsedTime];
//        [item setReportValue:taskMetricsP.totalConnectTime forKey:QNReportRequestKeyConnectElapsedTime];
//        [item setReportValue:taskMetricsP.totalSecureConnectTime forKey:QNReportRequestKeyTLSConnectElapsedTime];
//        [item setReportValue:taskMetricsP.totalRequestTime forKey:QNReportRequestKeyRequestElapsedTime];
//        [item setReportValue:taskMetricsP.totalWaitTime forKey:QNReportRequestKeyWaitElapsedTime];
//        [item setReportValue:taskMetricsP.totalWaitTime forKey:QNReportRequestKeyResponseElapsedTime];
//        [item setReportValue:taskMetricsP.totalResponseTime forKey:QNReportRequestKeyResponseElapsedTime];
//        [item setReportValue:self.requestInfo.fileOffset forKey:QNReportRequestKeyFileOffset];
//        [item setReportValue:taskMetricsP.bytesSend forKey:QNReportRequestKeyBytesSent];
//        [item setReportValue:taskMetricsP.totalBytes forKey:QNReportRequestKeyBytesTotal];
//        [item setReportValue:@([QNUtils getCurrentProcessID]) forKey:QNReportRequestKeyPid];
//        [item setReportValue:@([QNUtils getCurrentThreadID]) forKey:QNReportRequestKeyTid];
//        [item setReportValue:self.requestInfo.targetRegionId forKey:QNReportRequestKeyTargetRegionId];
//        [item setReportValue:self.requestInfo.currentRegionId forKey:QNReportRequestKeyCurrentRegionId];
//        [item setReportValue:info.requestReportErrorType forKey:QNReportRequestKeyErrorType];
//            NSString *errorDesc = info.requestReportErrorType ? info.message : nil;
//        [item setReportValue:errorDesc forKey:QNReportRequestKeyErrorDescription];
//        [item setReportValue:self.requestInfo.requestType forKey:QNReportRequestKeyUpType];
//        [item setReportValue:[QNUtils systemName] forKey:QNReportRequestKeyOsName];
//        [item setReportValue:[QNUtils systemVersion] forKey:QNReportRequestKeyOsVersion];
//        [item setReportValue:[QNUtils sdkLanguage] forKey:QNReportRequestKeySDKName];
//        [item setReportValue:kQiniuVersion forKey:QNReportRequestKeySDKVersion];
//        [item setReportValue:@([QNUtils currentTimestamp]) forKey:QNReportRequestKeyClientTime];
//        [item setReportValue:[QNUtils getCurrentNetworkType] forKey:QNReportRequestKeyNetworkType];
//        [item setReportValue:[QNUtils getCurrentSignalStrength] forKey:QNReportRequestKeySignalStrength];
    }

    public interface RequestCompleteHandler {
        public void complete(ResponseInfo responseInfo,
                             ArrayList<UploadSingleRequestMetrics> requestMetricsList,
                             JSONObject response);
    }
}

