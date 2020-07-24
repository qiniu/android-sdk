package com.qiniu.android.http.networkCheck;

import com.qiniu.android.storage.GlobalConfiguration;
import com.qiniu.android.utils.Utils;

import java.util.HashMap;


public class NetworkCheckManager {

    public static int NetworkCheckStatusA = 0;
    public static int NetworkCheckStatusB = 1;
    public static int NetworkCheckStatusC = 2;
    public static int NetworkCheckStatusD = 3;
    public static int NetworkCheckStatusUnknown = 4;

    private NetworkChecker networkChecker = new NetworkChecker();
    HashMap<String, String> checkingIPTypeInfo = new HashMap<>();
    HashMap<String, NetworkCheckStatusInfo> statusInfoDictionary = new HashMap<>();

    private final static NetworkCheckManager networkCheckManager = new NetworkCheckManager();
    private NetworkCheckManager(){
        networkChecker.networkCheckerListener = new NetworkChecker.NetworkCheckerListener() {
            @Override
            public void checkComplete(String ip, String host, long time) {
                checkCompleteAction(ip, host, time);
            }
        };
    }
    public static NetworkCheckManager getInstance(){
        return networkCheckManager;
    }

    public int getIPNetworkStatus(String ip, String host){
        if (GlobalConfiguration.getInstance().isCheckOpen == false){
            return NetworkCheckStatusUnknown;
        }

        String ipType = Utils.getIpType(ip, host);
        NetworkCheckStatusInfo statusInfo = statusInfoDictionary.get(ipType);
        if (statusInfo != null){
            return statusInfo.status;
        } else {
            return NetworkCheckStatusUnknown;
        }
    }

    public void preCheckIPNetworkStatus(String[] ipArray, String host){
        if (GlobalConfiguration.getInstance().isCheckOpen == false){
            return;
        }

        for (String ip : ipArray) {
            String ipType = Utils.getIpType(ip, host);
            if (ipType != null && statusInfoDictionary.get(ipType) == null && checkingIPTypeInfo.get(ipType) == null) {
                checkingIPTypeInfo.put(ipType, ip);
                networkChecker.checkIP(ip, host);
            }
        }
    }

    void checkCachedIPListNetworkStatus(){
        for (String ipType : statusInfoDictionary.keySet()) {
            NetworkCheckStatusInfo statusInfo = statusInfoDictionary.get(ipType);
            networkChecker.checkIP(statusInfo.checkedIP, statusInfo.checkedHost);
        }
    }

    private void checkCompleteAction(String ip, String host, long time){
        String ipType = Utils.getIpType(ip, host);
        if (ipType == null && ipType.length() == 0){
            return;
        }

        NetworkCheckStatusInfo statusInfo = new NetworkCheckStatusInfo();
        statusInfo.checkedHost = host;
        statusInfo.checkedIP = ip;
        statusInfo.status = getNetworkCheckStatus(time);
        statusInfoDictionary.put(ipType, statusInfo);
        checkingIPTypeInfo.remove(ipType);
    }

    private int getNetworkCheckStatus(long time){
        int status = NetworkCheckStatusUnknown;
        if (time < 1){
            status = NetworkCheckStatusUnknown;
        } else if (time < 150){
            status = NetworkCheckStatusA;
        } else if (time < 500){
            status = NetworkCheckStatusB;
        } else if (time < 2000){
            status = NetworkCheckStatusC;
        } else {
            status = NetworkCheckStatusD;
        }
        return status;
    }


    private class NetworkCheckStatusInfo {
        private int status;
        private String checkedIP;
        private String checkedHost;
    }
}
