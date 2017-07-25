package com.qiniu.android.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * Created by long on 2017/7/25.
 */

public final class Json {
    public static String encodeMap(Map map) {
        JSONObject obj = new JSONObject(map);
        return obj.toString();
    }

    public static String encodeList(List list) {
        JSONArray array = new JSONArray(list);
        return array.toString();
    }
}
