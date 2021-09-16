package com.qiniu.android.storage.serverConfig;

import com.qiniu.android.common.Config;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.http.request.RequestTransaction;
import com.qiniu.android.storage.UpToken;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class ServerConfigSynchronizer {
    private static String Token;
    private static RequestTransaction serverConfigTransaction;
    private static RequestTransaction serverUserConfigTransaction;

    static void setToken(String token) {
        Token = token;
    }

    static synchronized void getServerConfigFromServer(final ServerConfigHandler handler) {
        if (handler == null) {
            return;
        }

        RequestTransaction transaction = createServerConfigTransaction();
        if (transaction == null) {
            return;
        }

        transaction.serverConfig(true, new RequestTransaction.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                handler.handle(new ServerConfig(response));
                destroyServerConfigTransaction();
            }
        });
    }

    private static synchronized RequestTransaction createServerConfigTransaction() {
        if (serverConfigTransaction != null) {
            return null;
        }

        List<String> servers = new ArrayList<>();
        servers.add(Config.preQueryHost00);
        servers.add(Config.preQueryHost01);
        serverConfigTransaction = new RequestTransaction(servers, UpToken.getInvalidToken());
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
            return;
        }

        transaction.serverConfig(true, new RequestTransaction.RequestCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics, JSONObject response) {
                handler.handle(new ServerUserConfig(response));
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
        servers.add(Config.preQueryHost00);
        servers.add(Config.preQueryHost01);
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