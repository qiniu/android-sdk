package com.qiniu.android.http.networkStatus;

import com.qiniu.android.http.request.IUploadServer;

public class UploadServerNetworkStatus {

    public static IUploadServer getBetterNetworkServer(IUploadServer serverA, IUploadServer serverB) {
        return isServerNetworkBetter(serverA, serverB) ? serverA : serverB;
    }

    // 如果两个 Server 网速相同且类别相同优先使用 serverA，类别不同优先使用 Http3
    public static boolean isServerNetworkBetter(IUploadServer serverA, IUploadServer serverB) {
        if (serverA == null) {
            return false;
        } else if (serverB == null) {
            return true;
        }

        String serverTypeA = NetworkStatusManager.getNetworkStatusType(serverA.getHttpVersion(), serverA.getHost(), serverA.getIp());
        String serverTypeB = NetworkStatusManager.getNetworkStatusType(serverB.getHttpVersion(), serverB.getHost(), serverB.getIp());
        if (serverTypeA == null || serverTypeA.length() == 0) {
            return false;
        } else if (serverTypeB == null || serverTypeB.length() == 0) {
            return true;
        }

        NetworkStatusManager.NetworkStatus serverStatusA = NetworkStatusManager.getInstance().getNetworkStatus(serverTypeA);
        NetworkStatusManager.NetworkStatus serverStatusB = NetworkStatusManager.getInstance().getNetworkStatus(serverTypeB);

        int serverASpeed = serverStatusA.getSpeed();
        int serverBSpeed = serverStatusB.getSpeed();
        String serverAHttpVersion = serverA.getHttpVersion();
        String serverBHttpVersion = serverB.getHttpVersion();
        if (serverAHttpVersion == null) {
            serverAHttpVersion = "";
        }
        if (serverBHttpVersion == null) {
            serverBHttpVersion = "";
        }
        if (serverAHttpVersion.equals(IUploadServer.HttpVersion3) && !serverAHttpVersion.equals(serverBHttpVersion)) {
            if (serverASpeed < 200 && serverBSpeed == NetworkStatusManager.DefaultSpeed) {
                return true;
            } else if (serverASpeed > NetworkStatusManager.DefaultSpeed && serverBSpeed > 400) {
                return false;
            }
        } else if (serverBHttpVersion.equals(IUploadServer.HttpVersion3) && !serverAHttpVersion.equals(serverBHttpVersion)) {
            if (serverBSpeed < 200 && serverASpeed == NetworkStatusManager.DefaultSpeed) {
                return false;
            } else if (serverBSpeed > NetworkStatusManager.DefaultSpeed && serverASpeed > 400) {
                return true;
            }
        }
        return serverBSpeed <= serverASpeed;
    }
}
