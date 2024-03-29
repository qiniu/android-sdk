package com.qiniu.android.http.networkStatus;

import com.qiniu.android.http.request.IUploadServer;

/**
 * UploadServerNetworkStatus
 *
 * @hidden
 */
public class UploadServerNetworkStatus {

    private UploadServerNetworkStatus() {
    }

    /**
     * 获取网络状态较好的 server
     *
     * @param serverA serverA
     * @param serverB serverB
     * @return 网络状态较好的 server
     */
    public static IUploadServer getBetterNetworkServer(IUploadServer serverA, IUploadServer serverB) {
        return isServerNetworkBetter(serverA, serverB) ? serverA : serverB;
    }

    /**
     * serverA 网络状态是否较 serverB 好
     * 如果两个 Server 网速相同且类别相同优先使用 serverA，类别不同 HTTP/3 较好
     *
     * @param serverA serverA
     * @param serverB serverB
     * @return 是否较好
     */
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
