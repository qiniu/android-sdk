package com.qiniu.android.http.networkCheck;

import com.qiniu.android.utils.Utils;

import java.util.HashMap;

public class NetworkCheckManager {

    enum NetworkCheckStatus {
        A, B, C, D, Unknown
    }

    public boolean isCheckOpen = true;

    private NetworkChecker networkChecker = new NetworkChecker();
    HashMap<String, String> checkingIPTypeInfo = new HashMap<>();
    HashMap<String, NetworkCheckStatusInfo> statusInfoDictionary = new HashMap<>();

    private final static NetworkCheckManager networkCheckManager = new NetworkCheckManager();
    private NetworkCheckManager(){
        networkChecker.networkCheckerListener = new NetworkChecker.NetworkCheckerListener() {
            @Override
            public void checkComplete(String ip, String host, long time) {
                checkComplete(ip, host, time);
            }
        };
    }
    public static NetworkCheckManager getInstance(){
        return networkCheckManager;
    }


    public int maxCheckCount(){
        return networkChecker.maxCheckCount;
    }

    public void setMaxCheckCount(int maxCheckCount){
        networkChecker.maxCheckCount = maxCheckCount;
    }

    public int checkMaxTime(){
        return networkChecker.maxTime;
    }

    public void setCheckMaxTime(int checkMaxTime){
        networkChecker.maxTime = checkMaxTime;
    }


    public NetworkCheckStatus getIPNetworkStatus(String ip, String host){
        String ipType = Utils.getIpType(ip, host);
        NetworkCheckStatusInfo statusInfo = statusInfoDictionary.get(ipType);
        if (statusInfo != null){
            return statusInfo.status;
        } else {
            return NetworkCheckStatus.Unknown;
        }
    }

    public void preCheckIPNetworkStatus(String[] ipArray, String host){
        for (String ip : ipArray) {
            String ipType = Utils.getIpType(ip, host);
            if (ipType != null && statusInfoDictionary.get(ipType) != null && checkingIPTypeInfo.get(ipType) != null) {
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

    private void checkComplete(String ip, String host, long time){
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

    private NetworkCheckStatus getNetworkCheckStatus(long time){
        NetworkCheckStatus status = NetworkCheckStatus.Unknown;
        if (time < 1){
            status = NetworkCheckStatus.Unknown;
        } else if (time < 150){
            status = NetworkCheckStatus.A;
        } else if (time < 500){
            status = NetworkCheckStatus.B;
        } else if (time < 2000){
            status = NetworkCheckStatus.C;
        } else {
            status = NetworkCheckStatus.D;
        }
        return status;
    }


    private class NetworkCheckStatusInfo {
        private NetworkCheckStatus status;
        private String checkedIP;
        private String checkedHost;
    }
}
