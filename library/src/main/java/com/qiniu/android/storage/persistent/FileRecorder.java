package com.qiniu.android.storage.persistent;

import com.qiniu.android.storage.Recorder;
import com.qiniu.android.utils.UrlSafeBase64;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public final class FileRecorder implements Recorder {

    public String directory;

    public FileRecorder(String directory) throws IOException {
        this.directory = directory;
        File f = new File(directory);
        if (!f.exists()) {
            boolean r = f.mkdirs();
            if (!r) {
                throw new IOException("mkdir failed");
            }
            return;
        }
        if (!f.isDirectory()) {
            throw new IOException("does not mkdir");
        }
    }

    @Override
    public void set(String key, byte[] data) {
        File f = new File(directory, UrlSafeBase64.encodeToString(key));
        FileOutputStream fo = null;
        try {
            fo = new FileOutputStream(f);
            fo.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (fo != null) {
            try {
                fo.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public byte[] get(String key) {
        File f = new File(directory, UrlSafeBase64.encodeToString(key));
        FileInputStream fi = null;
        byte[] data = new byte[(int) f.length()];
        int read = 0;
        try {
            fi = new FileInputStream(f);
            read = fi.read(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (fi != null) {
            try {
                fi.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (read == 0) {
            return null;
        }
        return data;
    }

    @Override
    public void del(String key) {
        File f = new File(directory, UrlSafeBase64.encodeToString(key));
        f.delete();
    }
}
