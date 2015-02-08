package com.qiniu.android.storage;

import java.io.File;

/**
 * 本地持久化上传纪录key生成工具
 */
public interface KeyGenerator {
    /**
     * 根据服务器的key和本地文件名生成持久化纪录的key
     *
     * @param key  服务器的key
     * @param file 本地文件名
     * @return 持久化上传纪录的key
     */
    String gen(String key, File file);
}
