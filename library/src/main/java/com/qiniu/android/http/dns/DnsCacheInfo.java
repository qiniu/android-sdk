package com.qiniu.android.http.dns;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jemy on 2019/9/23.
 */
public class DnsCacheInfo implements java.io.Serializable {

    /**
     * DNS 缓存时间
     */
    private String currentTime;

    /**
     * 本地 IP
     */
    private String localIp;

    /**
     * DNS 解析结果
     */
    private ConcurrentHashMap<String, List<IDnsNetworkAddress>> info;

    /**
     * 根据 json 数据构造 DnsCacheInfo
     *
     * @param jsonData json 数据
     * @return DnsCacheInfo
     */
    public static DnsCacheInfo createDnsCacheInfoByData(byte[] jsonData) {
        if (jsonData == null) {
            return null;
        }

        JSONObject cacheInfoJSONObject = null;
        try {
            cacheInfoJSONObject = new JSONObject(new String(jsonData));
        } catch (Exception ignored) {
            return null;
        }

        String currentTime = null;
        String localIp = null;
        ConcurrentHashMap<String, List<IDnsNetworkAddress>> info = new ConcurrentHashMap<>();

        try {
            currentTime = cacheInfoJSONObject.getString("currentTime");
        } catch (Exception ignored) {
        }
        try {
            localIp = cacheInfoJSONObject.getString("localIp");
        } catch (Exception ignored) {
        }

        JSONObject infoMapJSONObject = null;
        try {
            infoMapJSONObject = cacheInfoJSONObject.getJSONObject("info");
        } catch (Exception ignored) {
        }

        if (currentTime == null || localIp == null || infoMapJSONObject == null) {
            return null;
        }

        for (Iterator<String> it = infoMapJSONObject.keys(); it.hasNext(); ) {
            String key = it.next();
            try {
                List<IDnsNetworkAddress> addressList = new ArrayList<>();
                JSONArray addressJSONArray = infoMapJSONObject.getJSONArray(key);
                for (int i = 0; i < addressJSONArray.length(); i++) {
                    DnsNetworkAddress address = DnsNetworkAddress.address(addressJSONArray.getJSONObject(i));
                    addressList.add(address);
                }
                if (addressList.size() > 0) {
                    info.put(key, addressList);
                }
            } catch (Exception ignored) {
            }
        }

        return new DnsCacheInfo(currentTime, localIp, info);
    }

    /**
     * 构造方法
     */
    public DnsCacheInfo() {
    }

    /**
     * 构造方法
     *
     * @param currentTime 当前时间
     * @param localIp     本地 IP
     * @param info        DNS 缓存数据
     */
    public DnsCacheInfo(String currentTime, String localIp, ConcurrentHashMap<String, List<IDnsNetworkAddress>> info) {
        this.currentTime = currentTime;
        this.localIp = localIp;
        this.info = info;
    }

    String getCurrentTime() {
        return currentTime;
    }

    String getLocalIp() {
        return localIp;
    }

    /**
     * 获取缓存数据
     *
     * @return 缓存数据
     */
    public ConcurrentHashMap<String, List<IDnsNetworkAddress>> getInfo() {
        return info;
    }

    void setCurrentTime(String currentTime) {
        this.currentTime = currentTime;
    }

    void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    /**
     * 设置缓存数据
     *
     * @param info 缓存数据
     */
    public void setInfo(ConcurrentHashMap<String, List<IDnsNetworkAddress>> info) {
        this.info = info;
    }

    /**
     * 获取缓存的 key
     *
     * @return 缓存的 key
     */
    public String cacheKey() {
        return localIp;
    }

    /**
     * 获取缓存的 json byte 数据
     *
     * @return 缓存的 json byte 数据
     */
    public byte[] toJsonData() {
        JSONObject cacheInfoJSONObject = new JSONObject();
        try {
            cacheInfoJSONObject.putOpt("currentTime", currentTime);
        } catch (JSONException ignored) {
        }
        try {
            cacheInfoJSONObject.putOpt("localIp", localIp);
        } catch (JSONException ignored) {
        }

        JSONObject infoMapJSONObject = new JSONObject();
        for (String key : info.keySet()) {
            List<IDnsNetworkAddress> addressList = info.get(key);

            JSONArray addressJSONArray = new JSONArray();
            if (addressList != null) {
                for (IDnsNetworkAddress address : addressList) {
                    if (address instanceof DnsNetworkAddress) {
                        try {
                            addressJSONArray.put(((DnsNetworkAddress) address).toJson());
                        } catch (Exception ignored) {
                        }
                    }
                }
            }

            if (addressJSONArray.length() > 0) {
                try {
                    infoMapJSONObject.put(key, addressJSONArray);
                } catch (JSONException ignored) {
                }
            }
        }
        try {
            cacheInfoJSONObject.putOpt("info", infoMapJSONObject);
        } catch (JSONException ignored) {
        }
        return cacheInfoJSONObject.toString().getBytes();
    }

    /**
     * toString
     *
     * @return String 信息
     */
    @Override
    public String toString() {
        return "{\"currentTime\":\"" + currentTime + "\", \"localIp\":\"" + localIp + "\"}";
    }
}
