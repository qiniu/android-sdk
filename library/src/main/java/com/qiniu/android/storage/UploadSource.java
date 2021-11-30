package com.qiniu.android.storage;

import java.io.IOException;

abstract class UploadSource {
    /**
     * 未知大小
     */
    static final long UnknownSourceSize = -1;

    /**
     * 获取资源唯一标识
     * 作为断点续传时判断是否为同一资源的依据之一；
     * 如果两个资源的 record key 和 资源唯一标识均相同则认为资源为同一资源，断点续传才会生效
     * 注：
     * 同一资源的数据必须完全相同，否则上传可能会出现异常
     *
     * @return 资源修改时间
     */
    abstract String getId();

    /**
     * 是否可以重新加载文件信息，也即是否可以重新读取信息
     * @return return
     */
    abstract boolean couldReloadSource();

    /**
     * 重新加载文件信息，以便于重新读取
     *
     * @return 重新加载是否成功
     */
    abstract boolean reloadSource();

    /**
     * 获取资源文件名
     * @return 资源文件名
     */
    abstract String getFileName();

    /**
     * 获取资源大小
     * 无法获取大小时返回 -1
     * 作用：
     * 1. 验证资源是否为同一资源
     * 2. 计算上传进度
     *
     * @return 资源大小
     */
    abstract long getSize();

    /**
     * 读取数据
     * 1. 返回 byte[] 可能为空，但不会为 null；
     * 2. 当 byte[] 大小和 dataSize 不同时，则源数据已经读取结束
     * 3. 读取异常时抛出 IOException
     * 4. 仅支持串行调用，且 dataOffset 依次递增
     *
     * @param dataSize 数据大小
     * @param dataOffset 数据偏移量
     * @return 数据
     * @throws IOException 异常
     */
    abstract byte[] readData(int dataSize, long dataOffset) throws IOException;

    /**
     * 关闭流
     */
    abstract void close();

    /**
     * 资源类型
     */
    abstract String getSourceType();
}
