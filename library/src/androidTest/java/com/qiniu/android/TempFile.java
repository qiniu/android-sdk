package com.qiniu.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * Created by bailong on 14/10/11.
 */
public final class TempFile {
    public static void remove(File f) {
        if (f == null){
            return;
        }
        f.delete();
    }

    public static File createFile(int kiloSize) throws IOException {
        return createFile(kiloSize, "qiniu_" + (1024 * kiloSize) + "k");
    }

    public static File createFile(int kiloSize, String fileName) throws IOException {
        FileOutputStream fos = null;
        try {
            long size = (long) (1024 * kiloSize);
            File f = File.createTempFile(fileName, ".tmp");
            f.createNewFile();
            fos = new FileOutputStream(f);
            byte[] b = getByte(1023 * 4);
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

//    public static byte[] createData(int kiloSize){
//
//        long size = (long) (1024 * kiloSize);
//        byte[] data = new byte[0];
//
//        byte[] b = getByte(1024);
//        long s = 0;
//        while (s < size) {
//            int l = (int) Math.min(b.length, size - s);
//            s += l;
//        }
//        return data;
//    }

    public static byte[] getByte(int len) {
        return getByte(len, 0);
    }
    public static byte[] getByte(int len, int index) {
        byte[] b = new byte[len];
        b[0] = (byte)(index & 0xFF);
        for (int i = 1; i < len; i++) {
            b[i] = 'b';
        }
        b[len - 2] = '\r';
        b[len - 1] = '\n';
        return b;
    }
}
