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

    private static SingleFlight<Boolean> singleFlight = new SingleFlight<>();

    public static boolean check() {

        final CheckResult result = new CheckResult();

        final Wait wait = new Wait();
        check(new CheckCompleteHandler() {
            @Override
            public void complete(boolean isConnected) {
                result.isConnected = isConnected;
                wait.stopWait();
            }
        });
        wait.startWait();

        return result.isConnected;
    }

    private static void check(final CheckCompleteHandler completeHandler) {

        try {
            singleFlight.perform("connect_check", new SingleFlight.ActionHandler<Boolean>() {
                @Override
                public void action(final SingleFlight.CompleteHandler<Boolean> singleFlightComplete) throws Exception {
                    checkAllHosts(new CheckCompleteHandler() {
                        @Override
                        public void complete(boolean isConnected) {
                            singleFlightComplete.complete(isConnected);
                        }
                    });
                }
            }, new SingleFlight.CompleteHandler<Boolean>() {
                @Override
                public void complete(Boolean value) {
                    completeHandler.complete(value);
                }
            });
        } catch (Exception e) {
            completeHandler.complete(true);
        }
    }

    private static void checkAllHosts(final CheckCompleteHandler completeHandler) {
        String[] allHosts = GlobalConfiguration.getInstance().connectCheckURLStrings;
        if (allHosts == null) {
            completeHandler.complete(true);
            return;
        }

        allHosts = allHosts.clone();
        final CheckStatus checkStatus = new CheckStatus();
        checkStatus.totalCount = allHosts.length;
        checkStatus.completeCount = 0;
        for (String host : allHosts) {
            checkHost(host, new CheckCompleteHandler() {
                @Override
                public void complete(boolean isHostConnected) {

                    synchronized (checkStatus) {
                        checkStatus.completeCount += 1;
                    }
                    if (isHostConnected) {
                        checkStatus.isConnected = true;
                    }
                    if (checkStatus.completeCount == checkStatus.totalCount) {
                        LogUtil.i("== check all hosts has completed totalCount:" + checkStatus.totalCount + " completeCount:" + checkStatus.completeCount);
                        completeHandler.complete(checkStatus.isConnected);
                    } else {
                        LogUtil.i("== check all hosts not completed totalCount:" + checkStatus.totalCount + " completeCount:" + checkStatus.completeCount);
                    }
                }
            });
        }

    }

    private static void checkHost(final String host, final CheckCompleteHandler completeHandler) {

        Request request = new Request(host, Request.HttpMethodHEAD, null, null, 3);
        SystemHttpClient client = new SystemHttpClient();

        LogUtil.i("== checkHost:" + host);
        client.request(request, true, null, null, new IRequestClient.RequestClientCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadSingleRequestMetrics metrics, JSONObject response) {
                if (responseInfo.statusCode > 99) {
                    LogUtil.i("== checkHost:" + host + " result: true");
                    completeHandler.complete(true);
                } else {
                    LogUtil.i("== checkHost:" + host + " result: false");
                    completeHandler.complete(false);
                }
            }
        });
    }


    private interface CheckCompleteHandler {
        void complete(boolean isConnected);
    }

    private static class CheckStatus {
        private int totalCount = 0;
        private int completeCount = 0;
        private boolean isConnected = false;
    }

    private static class CheckResult {
        private boolean isConnected = false;
    }
}
