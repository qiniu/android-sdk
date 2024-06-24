package com.qiniu.android.utils;

import java.util.List;

/**
 * List 工具类
 */
public class ListUtils {

    /**
     * 判断 List 是否为空
     *
     * @param objects List
     * @return 是否为空
     * @param <T> List 中的元素类型
     */
    public static <T> boolean isEmpty(List<T> objects) {
        return objects == null || objects.isEmpty();
    }
}
