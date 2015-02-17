package com.qiniu.android.storage;

/**
 * 定义分片上传时纪录上传进度的接口
 */
public interface Recorder {

    /**
     * 新建或更新文件分片上传的进度
     *
     * @param key  持久化的键
     * @param data 持久化的内容
     */
    void set(String key, byte[] data);

    /**
     * 获取文件分片上传的进度信息
     *
     * @param key 持久化的键
     * @return 对应的信息
     */
    byte[] get(String key);

    /**
     * 删除文件分片上传的进度文件
     *
     * @param key 持久化的键
     */
    void del(String key);
}
