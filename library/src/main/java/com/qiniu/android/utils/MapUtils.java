package com.qiniu.android.utils;

import java.util.Map;

/**
 * Map 工具类
 */
public class MapUtils {

    /**
     * 判断 Map 是否为空
     *
     * @param objects Map
     * @return 是否为空
     * @param <K> key 类型
     * @param <V> value 类型
     */
    public static <K,V> boolean isEmpty(Map<K,V> objects) {
        return objects == null || objects.isEmpty();
    }
}
