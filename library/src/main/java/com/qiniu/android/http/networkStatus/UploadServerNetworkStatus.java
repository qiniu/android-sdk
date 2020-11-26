package com.qiniu.android.http.networkStatus;

import com.qiniu.android.http.request.IUploadServer;
import com.qiniu.android.utils.Utils;

public class UploadServerNetworkStatus {

    public static boolean isServerSupportHTTP3(IUploadServer server) {
        if (server == null) {
            return false;
        }

        String serverType = Utils.getIpType(server.getIp(), server.getHost());
        if (serverType == null) {
            return false;
        }

        NetworkStatusManager.NetworkStatus status = NetworkStatusManager.getInstance().getNetworkStatus(serverType);
        return status.isSupportHTTP3();
    }

    public static IUploadServer getBetterNetworkServer(IUploadServer serverA, IUploadServer serverB) {
        if (serverA == null) {
            return serverB;
        } else if (serverB == null) {
            return serverA;
        }

        String serverTypeA = Utils.getIpType(serverA.getIp(), serverA.getHost());
        String serverTypeB = Utils.getIpType(serverB.getIp(), serverB.getHost());
        if (serverTypeA == null) {
            return serverB;
        } else if (serverTypeB == null) {
            return serverA;
        }

        NetworkStatusManager.NetworkStatus serverStatusA = NetworkStatusManager.getInstance().getNetworkStatus(serverTypeA);
        NetworkStatusManager.NetworkStatus serverStatusB = NetworkStatusManager.getInstance().getNetworkStatus(serverTypeB);

        return serverStatusB.getSpeed() < serverStatusA.getSpeed() ? serverA : serverB;
    }
}
