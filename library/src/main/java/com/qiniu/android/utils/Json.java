package com.qiniu.android.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Map;

/**
 * Created by long on 2017/7/25.
 *
 * @hidden
 */
public final class Json {

    private Json() {
    }

    /**
     * map 转 json
     *
     * @param map map
     * @return json string
     */
    public static String encodeMap(Map map) {
        JSONObject obj = new JSONObject(map);
        return obj.toString();
    }

    /**
     * Collection 转 json
     *
     * @param collection Collection
     * @return json string
     */
    public static String encodeList(Collection collection) {
        JSONArray array = new JSONArray(collection);
        return array.toString();
    }
}
