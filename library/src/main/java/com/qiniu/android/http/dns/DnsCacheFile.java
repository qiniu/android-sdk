package com.qiniu.android.http.dns;
import com.qiniu.android.storage.Recorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by jemy on 2019/9/17.
 */

public class DnsCacheFile implements Recorder {

    public String directory;
    public File f;

    public DnsCacheFile(String directory) throws IOException {
        if (directory == null) {
            throw new IOException("directory invalid");
        }

        this.directory = directory;
        f = new File(directory);

        if (!f.exists()) {
            boolean r = f.mkdirs();
            if (!r) {
                throw new IOException("mkdir failed");
            }
        }

        if (!f.isDirectory()) {
            throw new IOException("does not mkdir");
        }
    }

    /**
     * 设置DNS缓存
     *
     * @param key  缓存文件明
     * @param data 缓存数据
     */
    @Override
    public synchronized void set(String key, byte[] data) {
        File f = new File(directory, key);
        if (f.exists()) {
            f.delete();
        }

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

    /**
     * 获取缓存
     *
     * @param key 缓存文件名
     */
    @Override
    public synchronized byte[] get(String key) {
        File f = new File(directory, key);
        if (!f.exists()) {
            return null;
        }

        FileInputStream fi = null;
        byte[] data = null;
        int read = 0;
        try {
            data = new byte[(int) f.length()];
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

    //f.delete()=false时才会有fs.length>1的情况
    public String getFileName() {
        return "dnsCache";
    }

    @Override
    public synchronized void del(String key) {
        if (key != null) {
            File f = new File(directory, key);
            f.delete();
        }
    }

    synchronized void clearCache() throws IOException {
        if (f == null) {
            throw new IOException("directory invalid");
        }

        File[] subFiles = f.listFiles();
        if (subFiles != null && subFiles.length > 0) {
            for (File f : subFiles) {
                f.delete();
            }
        }
    }
}
