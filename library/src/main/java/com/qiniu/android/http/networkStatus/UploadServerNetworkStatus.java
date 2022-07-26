package com.qiniu.android.http.networkStatus;

import com.qiniu.android.http.request.IUploadServer;

public class UploadServerNetworkStatus {

    public static IUploadServer getBetterNetworkServer(IUploadServer serverA, IUploadServer serverB) {
        return isServerNetworkBetter(serverA, serverB) ? serverA : serverB;
    }

    // 如果两个 Server 网速相同优先使用 serverA
    public static boolean isServerNetworkBetter(IUploadServer serverA, IUploadServer serverB) {
        if (serverA == null) {
            return false;
        } else if (serverB == null) {
            return true;
        }

        String serverTypeA = NetworkStatusManager.getNetworkStatusType(serverA.getHttpVersion(), serverA.getHost(), serverA.getIp());
        String serverTypeB = NetworkStatusManager.getNetworkStatusType(serverA.getHttpVersion(), serverB.getHost(), serverB.getIp());
        if (serverTypeA == null || serverTypeA.length() == 0) {
            return false;
        } else if (serverTypeB == null || serverTypeB.length() == 0) {
            return true;
        }

        NetworkStatusManager.NetworkStatus serverStatusA = NetworkStatusManager.getInstance().getNetworkStatus(serverTypeA);
        NetworkStatusManager.NetworkStatus serverStatusB = NetworkStatusManager.getInstance().getNetworkStatus(serverTypeB);

        return serverStatusB.getSpeed() < serverStatusA.getSpeed();
    }
}
