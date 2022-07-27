package com.qiniu.android.http.networkStatus;

import com.qiniu.android.storage.FileRecorder;
import com.qiniu.android.storage.Recorder;
import com.qiniu.android.utils.AsyncRun;
import com.qiniu.android.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NetworkStatusManager {

    private static String kNetworkStatusDiskKey = "NetworkStatus:v1.0.0";

    private boolean hasInit = false;
    private boolean isHandlingNetworkInfoOfDisk = false;
    private Recorder recorder;
    private ConcurrentHashMap<String, NetworkStatus> networkStatusInfo;
    private static NetworkStatusManager networkStatusManager = new NetworkStatusManager();
    private final ExecutorService executorService = new ThreadPoolExecutor(1, 2,
            120L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());

    public static NetworkStatusManager getInstance() {
        networkStatusManager.initData();
        return networkStatusManager;
    }

    public synchronized void initData() {
        if (hasInit){
            return;
        }
        hasInit = true;
        networkStatusManager.networkStatusInfo = new ConcurrentHashMap<>();
        networkStatusManager.asyncRecoverNetworkStatusFromDisk();
    }

    @Deprecated
    public static String getNetworkStatusType(String host, String ip) {
        return Utils.getIpType(ip, host);
    }

    public static String getNetworkStatusType(String httpVersion, String host, String ip) {
        return Utils.getIpType(httpVersion, ip, host);
    }

    public NetworkStatus getNetworkStatus(String type) {
        if (type == null || type.length() == 0) {
            return null;
        }
        NetworkStatus status = networkStatusInfo.get(type);
        if (status == null) {
            status = new NetworkStatus();
        }
        return status;
    }

    public void updateNetworkStatus(String type, int speed) {
        if (type == null || type.length() == 0) {
            return;
        }
        NetworkStatus status = networkStatusInfo.get(type);
        if (status == null) {
            status = new NetworkStatus();
            networkStatusInfo.put(type, status);
        } else {
            speed = (int)((float)speed * 0.4  + (float)status.getSpeed() * 0.6);
        }
        status.setSpeed(speed);

        asyncRecordNetworkStatusInfo();
    }

    // ---------- 持久化 -----------
    private void asyncRecordNetworkStatusInfo() {
        synchronized (this) {
            if (isHandlingNetworkInfoOfDisk) {
                return;
            }
            isHandlingNetworkInfoOfDisk = true;
        }
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                recordNetworkStatusInfo();
                isHandlingNetworkInfoOfDisk = false;
            }
        });
    }

    private void asyncRecoverNetworkStatusFromDisk() {
        synchronized (this) {
            if (isHandlingNetworkInfoOfDisk) {
                return;
            }
            isHandlingNetworkInfoOfDisk = true;
        }
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                recoverNetworkStatusFromDisk();
                isHandlingNetworkInfoOfDisk = true;
            }
        });
    }

    private void recordNetworkStatusInfo() {

        setupRecorder();

        if (recorder == null || networkStatusInfo == null) {
            return;
        }

        JSONObject networkStatusInfoJson = new JSONObject();
        for (String key : networkStatusInfo.keySet()) {
            NetworkStatus status = networkStatusInfo.get(key);
            if (status != null) {
                try {
                    networkStatusInfoJson.put(key, status.toJson());
                } catch (Exception ignored) {
                }
            }
        }
        recorder.set(kNetworkStatusDiskKey, networkStatusInfoJson.toString().getBytes());
    }

    private void recoverNetworkStatusFromDisk() {

        setupRecorder();

        if (recorder == null || networkStatusInfo == null) {
            return;
        }

        byte[] networkStatusInfoData = recorder.get(kNetworkStatusDiskKey);
        JSONObject networkStatusInfoJSON = null;
        try {
            networkStatusInfoJSON = new JSONObject(new String(networkStatusInfoData));
        } catch (Exception ignored) {
            return;
        }

        for (Iterator<String> it = networkStatusInfoJSON.keys(); it.hasNext(); ) {
            String key = it.next();
            try {
                JSONObject statusJson = networkStatusInfoJSON.getJSONObject(key);
                NetworkStatus status = NetworkStatus.statusFromJson(statusJson);
                if (status != null) {
                    networkStatusInfo.put(key, status);
                }
            } catch (JSONException ignored) {
            }
        }
    }


    private synchronized void setupRecorder() {
        if (recorder == null) {
            try {
                recorder = new FileRecorder(Utils.sdkDirectory() + "/NetworkInfo");
            } catch (Exception ignored) {
            }
        }
    }

    protected static final int DefaultSpeed = 600;
    public static class NetworkStatus {

        private int speed = DefaultSpeed;

        public int getSpeed() {
            return speed;
        }

        public void setSpeed(int speed) {
            this.speed = speed;
        }

        private JSONObject toJson() {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("speed", speed);
            } catch (Exception ignored) {
            }
            return jsonObject;
        }

        private static NetworkStatus statusFromJson(JSONObject jsonObject) {
            if (jsonObject == null) {
                return null;
            }

            NetworkStatus status = new NetworkStatus();
            try {
                status.speed = jsonObject.getInt("speed");
            } catch (Exception ignored) {
            }
            return status;
        }
    }
}
