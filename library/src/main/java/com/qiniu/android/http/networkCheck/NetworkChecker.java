package com.qiniu.android.http.networkCheck;

import com.qiniu.android.storage.GlobalConfiguration;

import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

class NetworkChecker {

    protected NetworkCheckerListener networkCheckerListener;

    private Timer timer;
    private HashMap<String, NetworkDetecter> socketInfoDictionary = new HashMap<>();
    private HashMap<String, NetworkCheckerInfo> checkerInfoDictionary = new HashMap<>();

    protected NetworkChecker(){
    }

    protected boolean checkIP(String ip, String host){
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

        if (checkerInfo.shouldCheck(GlobalConfiguration.getInstance().maxCheckCount)) {
            ipCheckComplete(ip);
            return false;
        } else {
            return connect(ip);
        }
    }

    private boolean connect(String ip){
        NetworkCheckerInfo checkerInfo = checkerInfoDictionary.get(ip);
        if (checkerInfo == null){
            return false;
        }

        checkerInfo.start();

        // -----------------
        createTimer();

        return true;
    }

    private void disconnect(String ip){
        NetworkDetecter detecter = socketInfoDictionary.get(ip);
        if (detecter == null){
            return;
        }

        // -----------------
    }

    private void checkTimeout(){
        Date currentDate = new Date();
        for (String ip : checkerInfoDictionary.keySet()){
            NetworkCheckerInfo checkerInfo = checkerInfoDictionary.get(ip);
            if (checkerInfo.isTimeout(currentDate, GlobalConfiguration.getInstance().maxTime)){
                disconnect(ip);
                performCheckIFNeeded(ip);
            }
        }
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
            networkCheckerListener.checkComplete(ip, checkerInfo.host, Math.min(time, (long)GlobalConfiguration.getInstance().maxTime * 1000));
        }

        if (checkerInfoDictionary.size() == 0){
            invalidateTimer();
        }
    }


    private synchronized void createTimer(){
        if (timer != null){
            return;
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                checkTimeout();
            }
        }, 1);
    }

    private synchronized void invalidateTimer(){
        timer.cancel();
        timer = null;
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

        private boolean isTimeout(Date date, int maxTime){
            if (startDate == null || date == null || maxTime < 1){
                return true;
            }
            long time = (long)(date.getTime() - startDate.getTime() * 0.001);
            return time > maxTime;
        }
    }


    protected interface NetworkCheckerListener {
        void checkComplete(String ip, String host, long time);
    }
}
