package com.qiniu.android.utils;

import java.security.MessageDigest;

import com.qiniu.android.dns.util.Hex;

/**
 * MD5 util
 *
 * @hidden
 */
public class MD5 {

    private MD5() {
    }

    /**
     * MD5 加密
     *
     * @param data 带加密数据
     * @return 已加密数据
     */
    public static String encrypt(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(data);
            return Hex.encodeHexString(digest.digest());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
