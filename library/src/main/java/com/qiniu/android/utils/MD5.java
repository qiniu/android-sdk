package com.qiniu.android.utils;
import java.security.MessageDigest;
import com.qiniu.android.dns.util.Hex;

public class MD5 {

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
