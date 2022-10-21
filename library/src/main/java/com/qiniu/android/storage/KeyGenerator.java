package com.qiniu.android.storage;

import java.io.File;

/**
 * 本地持久化上传纪录key生成工具
 */
public interface KeyGenerator {
    /**
     * 根据服务器的 key 和本地文件名生成持久化纪录的 key
     *
     * @param key  服务器的 key
     * @param file 本地文件名
     * @return 持久化上传纪录的 key
     */
    @Deprecated
    String gen(String key, File file);

    /**
     * 根据服务器的key和本地文件唯一 ID 生成持久化纪录的key
     * 如果开启断点续传功能，请确保持久化纪录的 key 相同的文件一定是同一个
     * SDK 断点续传流程：
     * 1. 用户调用上传接口上传资源 A
     * 2. 根据资源 A 信息调用 {@link KeyGenerator#gen(String, String)} 生成持久化纪录的 key
     * 3. 根据生成持久化纪录的 key 获取本地缓存记录，无缓存则直接走新资源上传流程
     * 4. 解析缓存记录中的 sourceId 对比当前资源 A 的 sourceId，如果不同则走新资源上传流程
     * 5. 对比缓存资源的 size 和待上传资源 A 的 size，如果两个 size 均不为 -1 且不相等，
     * 则走新资源上传流程；size 等于 -1 时，资源 A 为 InputStream 且不知道文件流大小，不验证 size
     * 6. 断点续传生效，进入断点续传流程
     *
     * @param key      服务器的key
     * @param sourceId 本地文件 ID
     *                 File: fileName + modifyTime
     *                 Uri: fileName + modifyTime
     *                 InputStream: fileName
     * @return 持久化上传纪录的key
     */
    String gen(String key, String sourceId);
}
