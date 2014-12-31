package com.qiniu.android.utils;

import android.util.Base64;

import com.qiniu.android.common.Config;

import java.io.UnsupportedEncodingException;

/**
 * URL安全的Base64编码和解码
 */
public final class UrlSafeBase64 {

    /**
     * 编码数据
     */
    public static String encodeToString(String data) {
        try {
            return encodeToString(data.getBytes(Config.CHARSET));
        } catch (UnsupportedEncodingException e) {
            //never in
            e.printStackTrace();
        }
        return null;
    }

    public static String encodeToString(byte[] data) {
        return Base64.encodeToString(data, Base64.URL_SAFE | Base64.NO_WRAP);
    }

    /**
     * 解码数据
     */
    public static byte[] decode(String data) {
        return Base64.decode(data, Base64.URL_SAFE | Base64.NO_WRAP);
    }
}
