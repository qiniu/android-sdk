package com.qiniu.android.storage.persistent;

import com.qiniu.android.http.dns.DnsCacheKey;
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
        this.directory = directory;
        f = new File(directory);
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

    /**
     * 设置DNS缓存
     *
     * @param key  缓存文件明
     * @param data 缓存数据
     */
    @Override
    public void set(String key, byte[] data) {
        File[] fs = f.listFiles();
        if (fs == null) return;
        if (fs.length > 0) {
            for (int i = 0; i < fs.length; i++) {
                del(fs[i].getName());
            }
        }

        File f = new File(directory, key);
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
    public byte[] get(String key) {
        File f = new File(directory, key);
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
        File[] fs = f.listFiles();
        if (fs == null) return null;
        if (fs.length == 1) {
            return fs[0].getName();
        } else if (fs.length > 1) {
            String fileName = null;
            long cachetime = 0;
            for (int i = 1; i < fs.length; i++) {
                String key = fs[i].getName();
                DnsCacheKey cacheKey = DnsCacheKey.toCacheKey(key);
                if (cacheKey == null)
                    return null;
                long time = Long.parseLong(cacheKey.getCurrentTime());
                if (time > cachetime) {
                    del(fileName);
                    cachetime = time;
                    fileName = key;
                }
            }
            return fileName;
        }
        return null;
    }

    @Override
    public void del(String key) {
        if (key != null) {
            File f = new File(directory, key);
            f.delete();
        }
    }
}
