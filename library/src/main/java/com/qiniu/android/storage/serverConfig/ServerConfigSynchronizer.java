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
    private static String[] Servers;
    private static RequestTransaction serverConfigTransaction;
    private static RequestTransaction serverUserConfigTransaction;

    static void setToken(String token) {
        Token = token;
    }

    static void setServers(String[] servers) {
        Servers = servers;
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
        if (serverConfigTransaction != null) {
            return null;
        }

        UpToken token = UpToken.parse(Token);
        if (token == null) {
            token = UpToken.getInvalidToken();
        }

        List<String> servers = new ArrayList<>();
        servers.add(Config.preQueryHost00);
        servers.add(Config.preQueryHost01);
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

        List<String> servers = new ArrayList<>();
        if (Servers != null && Servers.length > 0) {
            servers.addAll(Arrays.asList(Servers));
        } else {
            servers.add(Config.preQueryHost00);
            servers.add(Config.preQueryHost01);
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
