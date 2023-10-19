package com.qiniu.android.utils;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.BaseTest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CacheTest extends BaseTest {

    @Test
    public void testCache() {
        Info info = new Info();
        info.foo = "foo";
        info.bar = 1;

        String key = "info_key";
        Cache cache = new Cache.Builder(Info.class)
                .setVersion("v1")
                .setFlushCount(1)
                .builder();

        cache.cache(key, info, false);


        // 1. 测试内存缓存
        Info memInfo = (Info) cache.cacheForKey(key);
        assertEquals("foo", memInfo.foo);

        // 2. 测试删除内存缓存
        cache.clearMemoryCache();
        memInfo = (Info) cache.cacheForKey(key);
        assertEquals(null, memInfo);

        // 3. 测试 load
        cache = new Cache.Builder(Info.class)
                .setVersion("v1")
                .setFlushCount(1)
                .builder();
        memInfo = (Info) cache.cacheForKey(key);
        assertEquals("foo", memInfo.foo);

        // 4. 测试清除磁盘缓存测试
        cache.clearDiskCache();
        cache = new Cache.Builder(Info.class)
                .setVersion("v1")
                .setFlushCount(1)
                .builder();
        memInfo = (Info) cache.cacheForKey(key);
        assertEquals(null, memInfo);

        // 5. 测试异步 flush
        cache.cache(key, info, false);

        try {
            Thread.sleep(3 * 1000);
        } catch (Exception e) {
        }

        cache = new Cache.Builder(Info.class)
                .setVersion("v1")
                .setFlushCount(1)
                .builder();
        memInfo = (Info) cache.cacheForKey(key);
        assertEquals("foo", memInfo.foo);
        assertEquals(1, memInfo.bar);
    }

    static class Info implements Cache.Object {
        String foo;
        int bar;


        @Override
        public JSONObject toJson() {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("foo", this.foo);
                jsonObject.put("bar", this.bar);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject;
        }

        @Override
        public Cache.Object initWithJsonObject(JSONObject jsonObject) {
            if (jsonObject == null) {
                return null;
            }

            Info info = new Info();
            info.foo = jsonObject.optString("foo");
            info.bar = jsonObject.optInt("bar");
            return info;
        }
    }
}
