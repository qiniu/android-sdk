package com.qiniu.android.http.connectCheck;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadSingleRequestMetrics;
import com.qiniu.android.http.request.IRequestClient;
import com.qiniu.android.http.request.Request;
import com.qiniu.android.http.request.httpclient.SystemHttpClient;
import com.qiniu.android.storage.GlobalConfiguration;
import com.qiniu.android.utils.LogUtil;
import com.qiniu.android.utils.SingleFlight;
import com.qiniu.android.utils.Wait;

import org.json.JSONObject;

public class ConnectChecker {

    private static SingleFlight<ResponseInfo> singleFlight = new SingleFlight<>();

    public static boolean isConnected(ResponseInfo responseInfo) {
        return responseInfo != null && responseInfo.statusCode > 99;
    }

    public static ResponseInfo check() {

        final CheckResult result = new CheckResult();

        final Wait wait = new Wait();
        check(new CheckCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo) {
                result.responseInfo = responseInfo;
                wait.stopWait();
            }
        });
        wait.startWait();

        return result.responseInfo;
    }

    private static void check(final CheckCompleteHandler completeHandler) {

        try {
            singleFlight.perform("connect_check", new SingleFlight.ActionHandler<ResponseInfo>() {
                @Override
                public void action(final SingleFlight.CompleteHandler<ResponseInfo> singleFlightComplete) throws Exception {
                    checkAllHosts(new CheckCompleteHandler() {
                        @Override
                        public void complete(ResponseInfo responseInfo) {
                            singleFlightComplete.complete(responseInfo);
                        }
                    });
                }
            }, new SingleFlight.CompleteHandler<ResponseInfo>() {
                @Override
                public void complete(ResponseInfo responseInfo) {
                    completeHandler.complete(responseInfo);
                }
            });
        } catch (Exception e) {
            completeHandler.complete(null);
        }
    }

    private static void checkAllHosts(final CheckCompleteHandler completeHandler) {
        String[] allHosts = GlobalConfiguration.getInstance().connectCheckURLStrings;
        if (allHosts == null) {
            completeHandler.complete(null);
            return;
        }

        allHosts = allHosts.clone();
        final CheckStatus checkStatus = new CheckStatus();
        checkStatus.totalCount = allHosts.length;
        checkStatus.completeCount = 0;
        checkStatus.isCompleted = false;
        for (String host : allHosts) {
            checkHost(host, new CheckCompleteHandler() {
                @Override
                public void complete(ResponseInfo responseInfo) {
                    boolean isHostConnected = isConnected(responseInfo);
                    synchronized (checkStatus) {
                        checkStatus.completeCount += 1;
                    }
                    if (isHostConnected) {
                        checkStatus.isConnected = true;
                    }
                    if (isHostConnected || checkStatus.completeCount == checkStatus.totalCount) {
                        synchronized (checkStatus) {
                            if (checkStatus.isCompleted) {
                                LogUtil.i("== check all hosts has completed totalCount:" + checkStatus.totalCount + " completeCount:" + checkStatus.completeCount);
                                return;
                            } else {
                                LogUtil.i("== check all hosts completed totalCount:" + checkStatus.totalCount + " completeCount:" + checkStatus.completeCount);
                                checkStatus.isCompleted = true;
                            }
                        }
                        completeHandler.complete(responseInfo);
                    } else {
                        LogUtil.i("== check all hosts not completed totalCount:" + checkStatus.totalCount + " completeCount:" + checkStatus.completeCount);
                    }
                }
            });
        }

    }

    private static void checkHost(final String host, final CheckCompleteHandler completeHandler) {

        Request request = new Request(host, Request.HttpMethodHEAD, null, null, GlobalConfiguration.getInstance().connectCheckTimeout);
        SystemHttpClient client = new SystemHttpClient();

        LogUtil.i("== checkHost:" + host);
        client.request(request, true, null, null, new IRequestClient.RequestClientCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadSingleRequestMetrics metrics, JSONObject response) {
                LogUtil.i("== checkHost:" + host + " responseInfo:" + responseInfo);
                completeHandler.complete(responseInfo);
            }
        });
    }


    private interface CheckCompleteHandler {
        void complete(ResponseInfo responseInfo);
    }

    private static class CheckStatus {
        private int totalCount = 0;
        private int completeCount = 0;
        private boolean isCompleted = false;
        private boolean isConnected = false;
    }

    private static class CheckResult {
        private ResponseInfo responseInfo;
    }
}
