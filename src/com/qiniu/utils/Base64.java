package com.qiniu.utils;

public class Base64 {
    public static String encode(String data) {
        return android.util.Base64.encodeToString(data.getBytes(),
            android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP);
    }
}
