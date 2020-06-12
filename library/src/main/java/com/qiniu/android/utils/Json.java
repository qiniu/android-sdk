package com.qiniu.android.utils;

//import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Map;

/**
 * Created by long on 2017/7/25.
 */

public final class Json {
    public static String encodeMap(Map map) {
        JSONObject obj = new JSONObject(map);
        return obj.toString();
    }

    public static String encodeList(Collection collection) {
        JSONArray array = new JSONArray(collection);
        return array.toString();
    }

    public static String object2Json(Object obj) {
        String jsonString =  "";
        try {
//             jsonString = new Gson().toJson(obj);
        }catch (Throwable t) {
            // do nothing
        }
        return jsonString;
    }
}
