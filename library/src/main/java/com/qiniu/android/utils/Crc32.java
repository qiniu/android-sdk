package com.qiniu.android.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.CRC32;

public final class Crc32 {
    public static long bytes(byte[] data, int offset, int length) {
        CRC32 crc32 = new CRC32();
        crc32.update(data, offset, length);
        return crc32.getValue();
    }

    public static long bytes(byte[] data) {
        return bytes(data, 0, data.length);
    }


    public static long file(File f) throws IOException {
        FileInputStream fi = new FileInputStream(f);
        byte[] buff = new byte[64 * 1024];
        int len;
        CRC32 crc32 = new CRC32();
        try {
            while ((len = fi.read(buff)) != -1) {
                crc32.update(buff, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            fi.close();
        }

        return crc32.getValue();
    }

}
