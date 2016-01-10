package com.qiniu.android.storage.persistent;

import com.qiniu.android.storage.Recorder;
import com.qiniu.android.utils.UrlSafeBase64;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

/**
 * 实现分片上传时上传进度的接口方法
 */
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

    /**
     * 纪录分片上传进度
     *
     * @param key  上传文件进度文件保存名
     * @param data 上传文件的进度数据
     */
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

    /**
     * 获取分片上传进度
     *
     * @param key 上传文件进度文件保存名
     */
    @Override
    public byte[] get(String key) {
        File f = new File(directory, UrlSafeBase64.encodeToString(key));
        FileInputStream fi = null;
        byte[] data = null;
        int read = 0;
        try {
            if (outOfDate(f)) {
                f.delete();
                return null;
            }
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

    private boolean outOfDate(File f) {
        return f.lastModified() + 1000 * 3600 * 24 * 2 < new Date().getTime();
    }

    /**
     * 删除已上传文件的进度文件
     *
     * @param key 上传文件进度文件保存名
     */
    @Override
    public void del(String key) {
        File f = new File(directory, UrlSafeBase64.encodeToString(key));
        f.delete();
    }
}
