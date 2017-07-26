package com.qiniu.android.bigdata.pipeline;

/**
 * Created by long on 2017/7/25.
 */

//import com.qiniu.android.utils.Json;

import com.qiniu.android.utils.Json;
import com.qiniu.android.utils.StringUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * 数据点
 */
public final class Point {
    private static final String rfc3339format = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    private static final TimeZone timezone = Calendar.getInstance().getTimeZone();
    public static int MAX_POINT_SIZE = 1 * 1024 * 1024;
    private List<Field> fields = new ArrayList<Field>();
    private int size;

    /**
     * 从文本中读取数据点
     *
     * @param pstr 类似 "a=1"
     * @return Point 解析数据点
     */
    static Point fromPointString(String pstr) {
        Point p = new Point();
        String ptrim = pstr.trim();
        if (ptrim.length() <= 0) {
            return p;
        }
        String[] parts = ptrim.split("\t");
        for (String part : parts) {
            String partTrim = part.trim();
            if (partTrim.length() <= 0) {
                // 空字段
                continue;
            }
            int i = partTrim.indexOf('=');
            if (i <= 0) {
                // 首字符是 = 或者不包含 =
                continue;
            }
            String key = partTrim.substring(0, i);
            String value = partTrim.substring(i + 1, part.length());
            p.append(key, value);
        }
        return p;
    }

    public static <V> Point fromPointMap(Map<String, V> data) {
        Point p = new Point();
        for (Map.Entry<String, V> it : data.entrySet()) {
            p.appendAny(it.getKey(), it.getValue());
        }
        return p;
    }

    public static Point fromPointObject(Object obj) {
        Class cls = obj.getClass();
        java.lang.reflect.Field[] fields = cls.getDeclaredFields();
        Point p = new Point();

        for (java.lang.reflect.Field f : fields) {
            Object v;
            try {
                v = f.get(obj);
            } catch (IllegalAccessException e) {
                continue;
            }
            p.appendAny(f.getName(), v);
        }
        return p;
    }

    /**
     * 从多行文本中读取多数据点
     *
     * @param points 类似 "a=1\na=3\na=5"
     * @return ret 数据点列表
     */
    public static List<Point> fromPointsString(String points) {
        List<Point> ret = new ArrayList<Point>();
        if (StringUtils.isBlank(points)) {
            return ret;
        }
        String[] ps = points.trim().split("\n");
        for (String line : ps) {
            if (StringUtils.isBlank(line)) {
                continue;
            }
            ret.add(fromPointString(line));
        }
        return ret;
    }

    void appendAny(String k, Object v) {
        if (v == null) {
            return;
        }
        if (v instanceof Integer || v instanceof Long
                || v instanceof Float || v instanceof Double || v instanceof Boolean) {
            append(new Point.Field(k, v));
        } else if (v instanceof String) {
            append(k, (String) v);
        } else if (v instanceof Collection) {
            append(k, (Collection) v);
        } else if (v instanceof Map) {
            append(k, (Map) v);
        } else if (v instanceof Date) {
            append(k, (Date) v);
        } else {
            append(k, v.toString());
        }
    }

    /**
     * 增加整数字段
     *
     * @param key   字段名
     * @param value 整数字段值
     */
    public void append(String key, Integer value) {
        Field field = new Field(key, value);
        append(field);
    }

    /**
     * 增加整数字段
     *
     * @param key   字段名
     * @param value 长整数字段值
     */
    public void append(String key, Long value) {
        Field field = new Field(key, value);
        append(field);
    }

    /**
     * 增加字符串字段
     *
     * @param key   字段名
     * @param value 字符串字段值
     */
    public void append(String key, String value) {
        String escapeValue = value.replace("\n", "\\n").replace("\t", "\\t");
        Field field = new Field(key, escapeValue);
        append(field);
    }

    /**
     * 增加浮点数字段
     *
     * @param key   字段名
     * @param value 双精度字段值
     */
    public void append(String key, Double value) {
        Field field = new Field(key, value);
        append(field);
    }

    /**
     * 增加浮点数字段
     *
     * @param key   字段名
     * @param value 浮点型字段值
     */
    public void append(String key, Float value) {
        Field field = new Field(key, value);
        append(field);
    }

    /**
     * 增加数组字段
     *
     * @param key   字段名
     * @param value 数组型字段值
     * @param <V>   数组元素类型
     */
    public <V> void append(String key, Collection<V> value) {
        Field field = new Field(key, Json.encodeList(value));
        append(field);
    }

    /**
     * 添加Map字段
     *
     * @param key   字段名
     * @param value Map型字段值
     * @param <K>   Map 键类型
     * @param <V>   Map 值类型
     */
    public <K, V> void append(String key, Map<K, V> value) {
        Field field = new Field(key, Json.encodeMap(value));
        append(field);
    }

    /**
     * 增加bool字段
     *
     * @param key   字段名
     * @param value 布尔型字段值
     */
    public void append(String key, Boolean value) {
        Field field = new Field(key, value);
        append(field);
    }

    /**
     * 增加时间字段
     *
     * @param key   字段名
     * @param value Date型字段值
     */
    public void append(String key, Date value) {
        DateFormat formatter = new SimpleDateFormat(rfc3339format);
        formatter.setTimeZone(timezone);
        String dstr = formatter.format(value);
        Field field = new Field(key, dstr);
        append(field);
    }

    private void append(Field f) {
        fields.add(f);
        size += f.getSize() + 1; // 包含字段本身和后缀的分隔符 \t \n
    }

    /**
     * 获得数据点的byte大小
     *
     * @return 数据点byte大小，整型
     */
    public int getSize() {
        return size;
    }

    /**
     * 判断数据点是否超过单点最大限制
     *
     * @return 单个数据点大小大于阈值
     */
    public boolean isTooLarge() {
        return size >= MAX_POINT_SIZE;
    }

    /**
     * 生成数据点
     *
     * @return 序列化数据点
     */
    public String toString() {
        StringBuilder buff = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            buff.append(String.valueOf(fields.get(i)));
            if (i == fields.size() - 1) {
                buff.append("\n");
            } else {
                buff.append("\t");
            }
        }
        return buff.toString();
    }

    public static class Field {
        private final String key;
        private final Object value;
        private final int size;

        public Field(String key, Object value) {
            this.key = key;
            this.value = value;
            this.size = toString().getBytes().length;
        }

        public int getSize() {
            return size;
        }

        public String toString() {
            return key + "=" + value.toString();
        }
    }
}