package com.qiniu.android.bigdata.pipeline;

/**
 * Created by long on 2017/7/25.
 */

import com.qiniu.android.utils.FastDatePrinter;
import com.qiniu.android.utils.Json;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 数据点
 */
public final class Points {
    private static String buildString(Object v) {
        if (v == null) {
            return null;
        }
        String str;
        if (v instanceof Integer || v instanceof Long
                || v instanceof Float || v instanceof Double || v instanceof Boolean) {
            str = v.toString();
        } else if (v instanceof String) {
            str = ((String) v).replace("\n", "\\n").replace("\t", "\\t");
        } else if (v instanceof Collection) {
            str = Json.encodeList((Collection) v);
        } else if (v instanceof Map) {
            str = Json.encodeMap((Map) v);
        } else if (v instanceof Date) {
            str = LazyHolder.INSTANCE.format((Date) v);
        } else {
            str = v.toString();
        }
        return str;
    }

    public static <V> StringBuilder formatPoint(Map<String, V> data, StringBuilder builder) {
        for (Map.Entry<String, V> it : data.entrySet()) {
            builder.append(it.getKey()).append("=").append(buildString(it.getValue())).append("\t");
        }
        builder.replace(builder.length() - 1, builder.length(), "\n");
        return builder;
    }

    public static StringBuilder formatPoint(Object obj, StringBuilder builder) {
        Class cls = obj.getClass();
        java.lang.reflect.Field[] fields = cls.getDeclaredFields();
        Points p = new Points();
        Map<String, Object> map = new HashMap<>();

        for (java.lang.reflect.Field f : fields) {
            Object v;
            try {
                v = f.get(obj);
            } catch (IllegalAccessException e) {
                continue;
            }
            map.put(f.getName(), v);
        }
        return formatPoint(map, builder);
    }

    public static <V> StringBuilder formatPoints(Map<String, V>[] data) {
        StringBuilder builder = new StringBuilder();
        for (Map<String, V> aData : data) {
            formatPoint(aData, builder);
        }
        return builder;
    }


    public static StringBuilder formatPoints(Object[] data) {
        StringBuilder builder = new StringBuilder();
        for (Object aData : data) {
            formatPoint(aData, builder);
        }
        return builder;
    }

    public static <V> StringBuilder formatPoints(List<Map<String, V>> data) {
        StringBuilder builder = new StringBuilder();
        for (Map<String, V> aData : data) {
            formatPoint(aData, builder);
        }
        return builder;
    }


    public static <V> StringBuilder formatPointsObjects(List<V> data) {
        StringBuilder builder = new StringBuilder();
        for (Object aData : data) {
            formatPoint(aData, builder);
        }
        return builder;
    }

    private static class LazyHolder {
        private static final FastDatePrinter INSTANCE = new FastDatePrinter(
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                Calendar.getInstance().getTimeZone(),
                Locale.getDefault());
    }

}