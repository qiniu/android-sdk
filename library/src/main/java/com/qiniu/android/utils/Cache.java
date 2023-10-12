package com.qiniu.android.utils;

import com.qiniu.android.storage.FileRecorder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Cache {

    // 当 cache 修改数量到达这个值时，就会 flush，默认是 1
    private final int flushCount;

    // 缓存被持久化为一个文件，此文件的文件名为 version，version 默认为：v1.0.0
    private final String version;

    private final Class<Object> objectClass;

    private final ConcurrentHashMap<String, Object> memCache = new ConcurrentHashMap<>();
    private final FileRecorder diskCache;

    public Cache(Class<Object> objectClass, int flushCount, String version) {

        this.objectClass = objectClass;
        this.flushCount = flushCount;
        this.version = version;

        FileRecorder fileRecorder = null;
        try {
            fileRecorder = new FileRecorder(Utils.sdkDirectory() + "/" + objectClass);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.diskCache = fileRecorder;

        this.load();
    }

    private void load() {
        if (this.diskCache == null) {
            return;
        }

        byte[] cacheData = this.diskCache.get(this.version);
        try {
            JSONObject cacheJson = new JSONObject(new String(cacheData));
            for (Iterator<String> it = cacheJson.keys(); it.hasNext(); ) {
                String key = it.next();
                try {
                    JSONObject jsonObject = cacheJson.getJSONObject(key);
                    Object object = objectClass.newInstance();
                    object.initWithJsonObject(jsonObject);
                    this.memCache.put(key, object);
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface Object {
        JSONObject toJson();

        void initWithJsonObject(JSONObject jsonObject);
    }

    public Object cacheForKey(String cacheKey) {
        return this.memCache.get(cacheKey);
    }

    public void cache(String cacheKey, Object object, boolean atomically) {
        this.memCache.put(cacheKey, object);
        this.flush(atomically);
    }

    public Map<String, Object> allMemoryCache() {
        return new HashMap<>(this.memCache);
    }

    public void flush(boolean atomically) {

    }

    private void flushCache(Map<String, Object> flushCache) {
        if (this.diskCache == null || flushCache == null || flushCache.isEmpty()) {
            return;
        }

        JSONObject cacheJson = new JSONObject();
        for (String key : flushCache.keySet()) {
            Object object = flushCache.get(key);
            try {
                cacheJson.put(key, object);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        byte[] cacheData = cacheJson.toString().getBytes();
        this.diskCache.set(this.version, cacheData);
    }

    public void clearMemoryCache() {
        this.memCache.clear();
    }

    public void clearDiskCache() {
        //this.diskCache.
    }
}
