package com.qiniu.android.utils;

import com.qiniu.android.storage.FileRecorder;

import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Cache {

    // 当 cache 修改数量到达这个值时，就会 flush，默认是 1
    private final int flushCount;

    // 缓存被持久化为一个文件，此文件的文件名为 version，version 默认为：v1.0.0
    private final String version;

    // 存储对象的类型
    private final Class<?> objectClass;

    // 内部
    private boolean isFlushing = false;
    private int needFlushCount = 0;

    private final ConcurrentHashMap<String, Object> memCache = new ConcurrentHashMap<>();
    private final FileRecorder diskCache;

    private Cache(Class<?> objectClass, int flushCount, String version) {
        this.objectClass = objectClass;
        this.flushCount = flushCount;
        this.version = version;

        FileRecorder fileRecorder = null;
        try {
            if (objectClass != null) {
                fileRecorder = new FileRecorder(Utils.sdkDirectory() + "/" + objectClass.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.diskCache = fileRecorder;

        this.load();
    }

    private void load() {
        if (this.diskCache == null || objectClass == null) {
            return;
        }

        byte[] cacheData = this.diskCache.get(this.version);
        if (cacheData == null || cacheData.length == 0) {
            return;
        }

        try {
            JSONObject cacheJson = new JSONObject(new String(cacheData));
            for (Iterator<String> it = cacheJson.keys(); it.hasNext(); ) {
                String key = it.next();
                try {
                    JSONObject jsonObject = cacheJson.getJSONObject(key);
                    Constructor<?> constructor = (Constructor<?>) objectClass.getConstructor(JSONObject.class);
                    Object object = (Object)constructor.newInstance(jsonObject);
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
    }

    public Object cacheForKey(String cacheKey) {
        return this.memCache.get(cacheKey);
    }

    public void cache(String cacheKey, Object object, boolean atomically) {
        if (StringUtils.isNullOrEmpty(cacheKey) || object == null) {
            return;
        }

        this.memCache.put(cacheKey, object);
        synchronized (this) {
            this.needFlushCount++;
        }

        if (this.needFlushCount >= this.flushCount) {
            this.flush(atomically);
        }
    }

    public Map<String, Object> allMemoryCache() {
        return new HashMap<>(this.memCache);
    }

    public void flush(boolean atomically) {
        synchronized (this) {
            if (this.isFlushing) {
                return;
            }

            this.isFlushing = true;
            this.needFlushCount = 0;
        }

        Map<String, Object> flushCache = new HashMap<>(this.memCache);
        if (atomically) {
            this.flushCache(flushCache);
        } else {
            AsyncRun.runInBack(new Runnable() {
                @Override
                public void run() {
                    flushCache(flushCache);
                }
            });
        }
    }

    private void flushCache(Map<String, Object> flushCache) {
        if (this.diskCache == null || flushCache == null || flushCache.isEmpty()) {
            return;
        }

        JSONObject cacheJson = new JSONObject();
        for (String key : flushCache.keySet()) {
            Object object = flushCache.get(key);
            if (object == null) {
                continue;
            }

            try {
                JSONObject jsonObject = object.toJson();
                if (jsonObject == null) {
                    continue;
                }
                cacheJson.put(key, jsonObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        byte[] cacheData = cacheJson.toString().getBytes();
        if (cacheData == null || cacheData.length == 0) {
            return;
        }

        this.diskCache.set(this.version, cacheData);

        synchronized (this) {
            isFlushing = false;
        }
    }

    public void clearMemoryCache() {
        this.memCache.clear();
    }

    public void clearDiskCache() {
        this.diskCache.deleteAll();
    }


    public static class Builder {
        // 当 cache 修改数量到达这个值时，就会 flush，默认是 1
        private int flushCount = 1;

        // 缓存被持久化为一个文件，此文件的文件名为 version，version 默认为：v1.0.0
        private String version = "v1.0.0";

        // 存储对象的类型
        private final Class<?> objectClass;

        public Builder(Class<?> objectClass) {
            this.objectClass = objectClass;
        }

        public Builder setFlushCount(int flushCount) {
            this.flushCount = flushCount;
            return this;
        }

        public Builder setVersion(String version) {
            this.version = version;
            return this;
        }

        public Cache builder() {
            return new Cache(this.objectClass, this.flushCount, this.version);
        }
    }
}
