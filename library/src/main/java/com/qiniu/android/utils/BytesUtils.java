package com.qiniu.android.utils;

import java.io.IOException;

public class BytesUtils {
    public static byte[] subBytes(byte[] source, int from, int length) throws IOException {
        if (length + from > source.length) {
            throw new IOException("copy bytes out of range");
        }

        byte[] buffer = new byte[length];
        System.arraycopy(source, from, buffer, 0, length);
        return buffer;
    }
}
