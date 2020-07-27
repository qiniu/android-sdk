package com.qiniu.android.http.networkCheck;

import com.qiniu.android.storage.GlobalConfiguration;

import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class NetworkChecker {

    NetworkCheckerListener networkCheckerListener;

    private ExecutorService networkDetectorService = Executors.newFixedThreadPool(1);
    private HashMap<String, NetworkDetector> detectorInfoDictionary = new HashMap<>();
    private HashMap<String, NetworkCheckerInfo> checkerInfoDictionary = new HashMap<>();

    NetworkChecker(){
    }

    boolean checkIP(String ip, String host){
        synchronized (this){
            if (ip == null || ip.length() == 0 || checkerInfoDictionary.get(ip) != null){
                return false;
            }
            NetworkCheckerInfo checkerInfo = new NetworkCheckerInfo(ip, host);
            checkerInfoDictionary.put(ip, checkerInfo);
        }
        return performCheckIFNeeded(ip);
    }

    private boolean performCheckIFNeeded(String ip){
        NetworkCheckerInfo checkerInfo = checkerInfoDictionary.get(ip);
        if (checkerInfo == null){
            return false;
        }

        checkerInfo.stop();

        if (!checkerInfo.shouldCheck(GlobalConfiguration.getInstance().maxCheckCount)) {
            ipCheckComplete(ip);
            return false;
        } else {
            return connect(ip);
        }
    }

    private boolean connect(final String ip){
        final NetworkCheckerInfo checkerInfo = checkerInfoDictionary.get(ip);
        if (checkerInfo == null){
            return false;
        }


        networkDetectorService.submit(new Runnable() {
            @Override
            public void run() {

                checkerInfo.start();

                NetworkDetector networkDetector = new NetworkDetector(ip, 80);
                detectorInfoDictionary.put(ip, networkDetector);
                boolean success = networkDetector.check(GlobalConfiguration.getInstance().maxCheckTime);
                if (success){
                    performCheckIFNeeded(ip);
                    detectorInfoDictionary.remove(ip);
                }
            }
        });

        return true;
    }


    private void ipCheckComplete(String ip){
        if (checkerInfoDictionary.get(ip) == null){
            return;
        }

        NetworkCheckerInfo checkerInfo = checkerInfoDictionary.get(ip);
        checkerInfo.stop();

        checkerInfoDictionary.remove(ip);

        if (networkCheckerListener != null){
            long time = checkerInfo.time / GlobalConfiguration.getInstance().maxCheckCount;
            networkCheckerListener.checkComplete(ip, checkerInfo.host, Math.min(time, (long)GlobalConfiguration.getInstance().maxCheckTime * 1000));
        }

    }



    private static class NetworkCheckerInfo {
        // 当前检测的次数
        private int count = 0;
        // 检测耗费时间
        private long time = 0;
        // 当前测试当前批次开始时间
        private Date startDate;
        private final String ip;
        private final String host;


        private NetworkCheckerInfo(String ip, String host) {
            this.ip = ip;
            this.host = host;
            this.count = 0;
            this.startDate = null;
        }

        private synchronized void start(){
            count += 1;
            startDate = new Date();
        }

        private synchronized void stop(){
            if (startDate == null){
                return;
            }
            time += (new Date().getTime() - startDate.getTime());
            startDate = null;
        }

        private boolean shouldCheck(int count){
            return count > this.count;
        }

    }


    interface NetworkCheckerListener {
        void checkComplete(String ip, String host, long time);
    }
}
