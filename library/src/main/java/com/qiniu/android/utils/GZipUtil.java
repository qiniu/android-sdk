package com.qiniu.android.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GZipUtil {

    public static byte[] gZip(String string){
        if (string == null){
            return null;
        }
        return gZip(string.getBytes());
    }

    public static byte[] gZip(byte[] bytes){
        if (bytes == null){
            return null;
        }
        if (bytes.length == 0){
            return bytes;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = null;
        try {
            gzip = new GZIPOutputStream(out);
            gzip.write(bytes);
        } catch (IOException e){
        } finally {
            if (gzip != null){
                try {
                    gzip.close();
                } catch (IOException e){
                }
            }
        }

        return out.toByteArray();
    }

    public static byte[] gUnzip(byte[] bytes) {
        if (bytes == null){
            return null;
        }
        if (bytes.length == 0){
            return bytes;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);

        GZIPInputStream gunzip = null;
        try {
            gunzip = new GZIPInputStream(in);
            byte[] buffer = new byte[256];
            int n;
            while ((n = gunzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
        } catch (IOException e) {
        } finally {
            if (gunzip != null) {
                try {
                    gunzip.close();
                } catch (IOException e) {
                }
            }
        }

        return out.toByteArray();
    }
}
