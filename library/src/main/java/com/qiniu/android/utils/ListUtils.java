package com.qiniu.android.utils;

import java.util.List;

public class ListUtils {

    public static <T> boolean isEmpty(List<T> objects) {
        return objects == null || objects.isEmpty();
    }
}
