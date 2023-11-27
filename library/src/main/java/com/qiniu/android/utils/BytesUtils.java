package com.qiniu.android.utils;

import java.io.IOException;

/**
 * Bytes 工具
 */
public class BytesUtils {

    /**
     * 获取 byte 数组的子数组
     *
     * @param source 源 byte 数组
     * @param from   子数组开始位置
     * @param length 子数组长度
     * @return 子数组
     * @throws IOException 异常
     */
    public static byte[] subBytes(byte[] source, int from, int length) throws IOException {
        if (length + from > source.length) {
            throw new IOException("copy bytes out of range");
        }

        byte[] buffer = new byte[length];
        System.arraycopy(source, from, buffer, 0, length);
        return buffer;
    }
}
