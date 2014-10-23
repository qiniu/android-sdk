package com.qiniu.android.utils;

import com.qiniu.android.common.Config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Etag {
    public static String data(byte[] data, int offset, int length) {
        try {
            return stream(new ByteArrayInputStream(data, offset, length), length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // never reach
        return null;
    }

    public static String data(byte[] data) {
        return data(data, 0, data.length);
    }

    public static String file(File file) throws IOException {
        FileInputStream fi = new FileInputStream(file);
        return stream(fi, file.length());
    }

    public static String file(String filePath) throws IOException {
        File f = new File(filePath);
        return file(f);
    }

    public static String stream(InputStream in, long len) throws IOException {
        if (len == 0) {
            return "Fto5o-5ea0sNMlW_75VgGJCv2AcJ";
        }
        byte[] buffer = new byte[64 * 1024];
        byte[][] blocks = new byte[(int) (len + Config.BLOCK_SIZE - 1) / Config.BLOCK_SIZE][];
        for (int i = 0; i < blocks.length; i++) {
            long left = len - (long) Config.BLOCK_SIZE * i;
            long read = left > Config.BLOCK_SIZE ? Config.BLOCK_SIZE : left;
            blocks[i] = oneBlock(buffer, in, (int) read);
        }
        return resultEncode(blocks);
    }

    private static byte[] oneBlock(byte[] buffer, InputStream in, int len) throws IOException {
        MessageDigest sha1 = null;
        try {
            sha1 = MessageDigest.getInstance("sha-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            //never reach
            return null;
        }
        int buffSize = buffer.length;
        while (len != 0) {
            int next = buffSize > len ? len : buffSize;
            //noinspection ResultOfMethodCallIgnored
            in.read(buffer, 0, next);
            sha1.update(buffer, 0, next);
            len -= next;
        }

        return sha1.digest();
    }

    private static String resultEncode(byte[][] sha1s) {
        byte head = 0x16;
        byte[] finalHash = sha1s[0];
        int len = finalHash.length;
        byte[] ret = new byte[len + 1];
        if (sha1s.length != 1) {
            head = (byte) 0x96;
            MessageDigest sha1 = null;
            try {
                sha1 = MessageDigest.getInstance("sha-1");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                // never reach
                return null;
            }
            for (byte[] s : sha1s) {
                sha1.update(s);
            }
            finalHash = sha1.digest();
        }
        ret[0] = head;
        System.arraycopy(finalHash, 0, ret, 1, len);
        return UrlSafeBase64.encodeToString(ret);
    }
}
