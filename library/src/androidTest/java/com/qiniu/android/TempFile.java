package com.qiniu.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by bailong on 14/10/11.
 */
public final class TempFile {
    public static void remove(File f) {
        f.delete();
    }

    public static File createFile(int kiloSize) throws IOException {
        FileOutputStream fos = null;
        try {
            long size = (long) (1024 * kiloSize);
            File f = File.createTempFile("qiniu_" + kiloSize + "k", "tmp");
            f.createNewFile();
            fos = new FileOutputStream(f);
            byte[] b = getByte(1024 * 4);
            long s = 0;
            while (s < size) {
                int l = (int) Math.min(b.length, size - s);
                fos.write(b, 0, l);
                s += l;
            }
            fos.flush();
            return f;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static byte[] getByte(int len) {
        byte[] b = new byte[len];
        b[0] = 'A';
        for (int i = 1; i < len; i++) {
            b[i] = 'b';
        }
        b[len - 2] = '\r';
        b[len - 1] = '\n';
        return b;
    }
}
