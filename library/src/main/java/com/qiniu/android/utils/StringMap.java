package com.qiniu.android.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * string map
 */
public final class StringMap {
    private Map<String, Object> map;

    /**
     * 构造函数
     */
    public StringMap() {
        this(new HashMap<String, Object>());
    }

    /**
     * 构造函数
     *
     * @param map map
     */
    public StringMap(Map<String, Object> map) {
        this.map = map;
    }

    /**
     * 新增键值对
     *
     * @param key   键
     * @param value 值
     * @return StringMap
     */
    public StringMap put(String key, Object value) {
        map.put(key, value);
        return this;
    }

    /**
     * 新增非空键值对
     *
     * @param key   键
     * @param value 值
     * @return StringMap
     */
    public StringMap putNotEmpty(String key, String value) {
        if (!StringUtils.isNullOrEmpty(value)) {
            map.put(key, value);
        }
        return this;
    }

    /**
     * 新增非空键值对
     *
     * @param key   键
     * @param value 值
     * @return StringMap
     */
    public StringMap putNotNull(String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
        return this;
    }


    /**
     * 新增键值对
     *
     * @param key  键
     * @param val  值
     * @param when 条件
     * @return StringMap
     */
    public StringMap putWhen(String key, Object val, boolean when) {
        if (when) {
            map.put(key, val);
        }
        return this;
    }

    /**
     * 新增键值对
     *
     * @param map map
     * @return StringMap
     */
    public StringMap putAll(Map<String, Object> map) {
        this.map.putAll(map);
        return this;
    }

    /**
     * 新增键值对
     *
     * @param map map
     * @return StringMap
     */
    public StringMap putFileds(Map<String, String> map) {
        this.map.putAll(map);
        return this;
    }

    /**
     * 新增键值对
     *
     * @param map map
     * @return StringMap
     */
    public StringMap putAll(StringMap map) {
        this.map.putAll(map.map);
        return this;
    }

    /**
     * 遍历
     *
     * @param imp Consumer
     */
    public void forEach(Consumer imp) {
        for (Map.Entry<String, Object> i : map.entrySet()) {
            imp.accept(i.getKey(), i.getValue());
        }
    }

    /**
     * 获取 map 大小
     *
     * @return map 大小
     */
    public int size() {
        return map.size();
    }

    /**
     * 获取 map
     *
     * @return map
     */
    public Map<String, Object> map() {
        return this.map;
    }

    /**
     * 获取 value
     *
     * @param key key
     * @return value
     */
    public Object get(String key) {
        return map.get(key);
    }

    /**
     * 获取 string
     *
     * @return string
     */
    public String formString() {
        final StringBuilder b = new StringBuilder();
        forEach(new Consumer() {
            private boolean notStart = false;

            @Override
            public void accept(String key, Object value) {
                if (notStart) {
                    b.append("&");
                }
                try {
                    b.append(URLEncoder.encode(key, "UTF-8")).append('=')
                            .append(URLEncoder.encode(value.toString(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new AssertionError(e);
                }
                notStart = true;
            }
        });
        return b.toString();
    }

    /**
     * 遍历回调
     */
    public interface Consumer {

        /**
         * 遍历回调
         *
         * @param key   key
         * @param value value
         */
        void accept(String key, Object value);
    }
}
