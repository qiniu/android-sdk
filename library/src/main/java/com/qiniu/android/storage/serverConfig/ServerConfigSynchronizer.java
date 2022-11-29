package com.qiniu.android.storage.serverConfig;

import com.qiniu.android.common.Config;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.http.request.RequestTransaction;
import com.qiniu.android.storage.UpToken;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class ServerConfigSynchronizer {
    private static String Token;
    private static String[] Hosts;
    private static RequestTransaction serverConfigTransaction;
    private static RequestTransaction serverUserConfigTransaction;

    static void setToken(String token) {
        Token = token;
    }

    static void setHosts(String[] hosts) {
        Hosts = hosts;
    }

    static void getServerConfigFromServer(final ServerConfigHandler handler) {
        if (handler == null) {
            return;
        }

        RequestTransaction transaction = createServerConfigTransaction();
        if (transaction == null) {
            handler.handle(null);
            return;
        }

        transaction.serverConfig(true, new RequestTransaction.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                if (responseInfo.isOK() && response != null) {
                    handler.handle(new ServerConfig(response));
                } else {
                    handler.handle(null);
                }
                destroyServerConfigTransaction();
            }
        });
    }

    private static synchronized RequestTransaction createServerConfigTransaction() {
        // 只上传传才会有 Token，当有上传时才做请求，避免不必要的请求
        if (serverConfigTransaction != null) {
            return null;
        }

        UpToken token = UpToken.parse(Token);
        if (token == null) {
            token = UpToken.getInvalidToken();
        }

        List<String> servers = null;
        if (Hosts != null && Hosts.length > 0) {
            servers = Arrays.asList(Hosts);
        } else {
            servers = Arrays.asList(Config.preQueryHosts());
        }
        serverConfigTransaction = new RequestTransaction(servers, token);
        return serverConfigTransaction;
    }

    private static synchronized void destroyServerConfigTransaction() {
        serverConfigTransaction = null;
    }


    static void getServerUserConfigFromServer(final ServerUserConfigHandler handler) {
        if (handler == null) {
            return;
        }

        RequestTransaction transaction = createServerUserConfigTransaction();
        if (transaction == null) {
            handler.handle(null);
            return;
        }

        transaction.serverUserConfig(true, new RequestTransaction.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                if (responseInfo.isOK() && response != null) {
                    handler.handle(new ServerUserConfig(response));
                } else {
                    handler.handle(null);
                }
                destroyServerUserConfigTransaction();
            }
        });
    }

    private static synchronized RequestTransaction createServerUserConfigTransaction() {
        if (serverUserConfigTransaction != null || Token == null) {
            return null;
        }

        UpToken token = UpToken.parse(Token);
        if (token == null || !token.isValid()) {
            return null;
        }

        List<String> servers = null;
        if (Hosts != null && Hosts.length > 0) {
            servers = Arrays.asList(Hosts);
        } else {
            servers = Arrays.asList(Config.preQueryHosts());
        }
        serverUserConfigTransaction = new RequestTransaction(servers, token);
        return serverUserConfigTransaction;
    }

    private static synchronized void destroyServerUserConfigTransaction() {
        serverUserConfigTransaction = null;
    }

    interface ServerConfigHandler {
        void handle(ServerConfig config);
    }

    interface ServerUserConfigHandler {
        void handle(ServerUserConfig config);
    }
}
