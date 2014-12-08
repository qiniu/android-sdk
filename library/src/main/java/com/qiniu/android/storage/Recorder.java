package com.qiniu.android.storage;

/**
 *  定义分片上传时纪录上传进度的接口
 */
public interface Recorder {

    /**
     *  新建或更新文件分片上传的进度
     * */
    void set(String key, byte[] data);

    /**
     *  获取文件分片上传的进度信息
     */
    byte[] get(String key);

    /**
     *  删除文件分片上传的进度文件
     */
    void del(String key);
}
