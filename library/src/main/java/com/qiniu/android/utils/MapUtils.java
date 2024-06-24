package com.qiniu.android.utils;

import java.util.Map;

public class MapUtils {
    public static <K,V> boolean isEmpty(Map<K,V> objects) {
        return objects == null || objects.isEmpty();
    }
}
