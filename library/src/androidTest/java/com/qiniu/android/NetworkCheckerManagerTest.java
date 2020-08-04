package com.qiniu.android;

import com.qiniu.android.http.networkCheck.NetworkCheckManager;
import com.qiniu.android.http.networkCheck.NetworkCheckTransaction;
import com.qiniu.android.storage.GlobalConfiguration;
import com.qiniu.android.utils.LogUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class NetworkCheckerManagerTest extends BaseTest {

    public void testCheckCount(){

        GlobalConfiguration.getInstance().maxCheckCount = 100;

        String host = "up.qiniu.com";
        String[] ipArray = new String[]{"180.101.136.87", "180.101.136.88", "180.101.136.89", "122.224.95.105",
                                        "115.238.101.32", "115.238.101.33", "115.238.101.34", "115.238.101.35",
                                        "115.238.101.36", "180.101.136.11", "180.101.136.31", "180.101.136.29",
                                        "180.101.136.28", "180.101.136.12", "180.101.136.33", "180.101.136.30",
                                        "180.101.136.32", "122.224.95.103", "122.224.95.108", "180.101.136.86"};
        NetworkCheckTransaction.addCheckSomeIPNetworkStatusTransaction(ipArray, host);

        getIPListNetworkStatus(new String[]{"180.101.136.87"}, host);

        wait(null, 10);

        getIPListNetworkStatus(ipArray, host);

    }

    public void testIPArray(){

        GlobalConfiguration.getInstance().maxCheckCount = 10;

        String host = "up.qiniu.com";
        String[] ipArray = new String[]{"180.101.136.87", "180.101.136.88", "180.101.136.89", "122.224.95.105",
                "115.238.101.32", "115.238.101.33", "115.238.101.34", "115.238.101.35",
                "115.238.101.36", "180.101.136.11", "180.101.136.31", "180.101.136.29",
                "180.101.136.28", "180.101.136.12", "180.101.136.33", "180.101.136.30",
                "180.101.136.32", "122.224.95.103", "122.224.95.108", "180.101.136.86"};

        ArrayList<String> ipList = new ArrayList<>();
        for (int i=0; i<5; i++) {
            ipList.addAll(Arrays.asList(ipArray));
        }

        NetworkCheckTransaction.addCheckSomeIPNetworkStatusTransaction(ipList.toArray(new String[ipList.size()]), host);

        getIPListNetworkStatus(new String[]{"180.101.136.87"}, host);

        wait(null, 30);

        getIPListNetworkStatus(ipArray, host);
    }

    public void testMaxTime(){

        GlobalConfiguration.getInstance().maxCheckTime = 5;
        GlobalConfiguration.getInstance().maxCheckCount = 3;

        String host = "up.qiniu.com";
        String[] ipArray = new String[]{"183.101.136.32", "123.224.95.103", "123.224.95.108", "183.101.136.86"};

        ArrayList<String> ipList = new ArrayList<>();
        for (int i=0; i<5; i++) {
            ipList.addAll(Arrays.asList(ipArray));
        }

        NetworkCheckTransaction.addCheckSomeIPNetworkStatusTransaction(ipList.toArray(new String[ipList.size()]), host);

        getIPListNetworkStatus(new String[]{"180.101.136.87"}, host);

        wait(null, 17);

        getIPListNetworkStatus(ipArray, host);
    }

    private  void getIPListNetworkStatus(String[] ipArray, String host){
        for (String ip : ipArray) {
            int status = NetworkCheckManager.getInstance().getIPNetworkStatus(ip, host);
            String statusString = new String[]{"A", "B", "C", "D", "Unknown"}[status];
            LogUtil.i("host:" + host + ", ip:" + ip + ", status:" + statusString);
        }
    }
}
