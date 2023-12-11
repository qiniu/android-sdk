package com.qiniu.android.http.networkStatus;

import com.qiniu.android.utils.Cache;
import com.qiniu.android.utils.Utils;

import org.json.JSONObject;


/**
 * 网络状态管理器
 *
 * @hidden
 */
public class NetworkStatusManager {

    private static final NetworkStatusManager networkStatusManager = new NetworkStatusManager();

    private final Cache cache = new Cache.Builder(NetworkStatus.class)
            .setVersion("v2")
            .setFlushCount(10)
            .builder();

    /**
     * 获取单例
     *
     * @return NetworkStatusManager 单例
     */
    public static NetworkStatusManager getInstance() {
        return networkStatusManager;
    }

    private NetworkStatusManager() {
    }

    /**
     * 获取网络状态唯一标识
     *
     * @param host host
     * @param ip   ip
     * @return 唯一标识
     */
    @Deprecated
    public static String getNetworkStatusType(String host, String ip) {
        return Utils.getIpType(ip, host);
    }

    /**
     * 获取网络状态唯一标识
     *
     * @param httpVersion httpVersion
     * @param host        host
     * @param ip          ip
     * @return 唯一标识
     */
    public static String getNetworkStatusType(String httpVersion, String host, String ip) {
        return Utils.getIpType(httpVersion, ip, host);
    }

    /**
     * 获取网络状态
     *
     * @param type 网络状态唯一标识
     * @return 网络状态
     */
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

    /**
     * 更新网络状态
     *
     * @param type  网络状态唯一标识
     * @param speed 网速
     */
    public void updateNetworkStatus(String type, int speed) {
        if (type == null || type.isEmpty()) {
            return;
        }

        NetworkStatus status = new NetworkStatus();
        status.speed = speed;
        this.cache.cache(type, status, false);
    }

    /**
     * default speed
     */
    protected static final int DefaultSpeed = 600;

    /**
     * 网络状态
     *
     * @hidden
     */
    public static class NetworkStatus implements Cache.Object {

        private int speed = DefaultSpeed;

        /**
         * 获取速度
         *
         * @return 速度
         */
        public int getSpeed() {
            return speed;
        }

        /**
         * 设置速度
         *
         * @param speed speed
         */
        public void setSpeed(int speed) {
            this.speed = speed;
        }

        private NetworkStatus() {
        }

        /**
         * 构造函数
         *
         * @param jsonObject 网络状态 json 数据
         */
        public NetworkStatus(JSONObject jsonObject) {
            if (jsonObject == null) {
                return;
            }

            try {
                this.speed = jsonObject.getInt("speed");
            } catch (Exception ignored) {
            }
        }

        /**
         * 获取 json 数据
         *
         * @return json 数据
         */
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
