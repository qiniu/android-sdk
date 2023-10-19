package com.qiniu.android.http.networkStatus;

import com.qiniu.android.utils.Cache;
import com.qiniu.android.utils.Utils;

import org.json.JSONObject;


public class NetworkStatusManager {

    private static final NetworkStatusManager networkStatusManager = new NetworkStatusManager();

    private final Cache cache = new Cache.Builder(NetworkStatus.class)
            .setVersion("v1.0.2")
            .setFlushCount(10)
            .builder();

    public static NetworkStatusManager getInstance() {
        return networkStatusManager;
    }

    @Deprecated
    public static String getNetworkStatusType(String host, String ip) {
        return Utils.getIpType(ip, host);
    }

    public static String getNetworkStatusType(String httpVersion, String host, String ip) {
        return Utils.getIpType(httpVersion, ip, host);
    }

    public NetworkStatus getNetworkStatus(String type) {
        if (type == null || type.isEmpty()) {
            return null;
        }

        Cache.Object object = cache.cacheForKey(type);
        if (object instanceof NetworkStatus) {
            return (NetworkStatus) object;
        } else {
            return new NetworkStatus();
        }
    }

    public void updateNetworkStatus(String type, int speed) {
        if (type == null || type.isEmpty()) {
            return;
        }

        NetworkStatus status = new NetworkStatus();
        status.speed = speed;
        this.cache.cache(type, status, false);
    }

    protected static final int DefaultSpeed = 600;

    public static class NetworkStatus implements Cache.Object {

        private int speed = DefaultSpeed;

        public int getSpeed() {
            return speed;
        }

        public void setSpeed(int speed) {
            this.speed = speed;
        }

        private NetworkStatus() {
        }

        public NetworkStatus(JSONObject jsonObject) {
            if (jsonObject == null) {
                return;
            }

            try {
                this.speed = jsonObject.getInt("speed");
            } catch (Exception ignored) {
            }
        }

        @Override
        public JSONObject toJson() {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("speed", speed);
            } catch (Exception ignored) {
            }
            return jsonObject;
        }
    }
}
