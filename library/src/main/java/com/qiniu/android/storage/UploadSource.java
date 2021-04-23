package com.qiniu.android.storage;

import java.io.IOException;

interface UploadSource {

    /**
     * 获取资源唯一标识
     * 作为断点续传时判断是否为同一资源的依据之一；
     * 如果两个资源的 record key 和 资源唯一标识均相同则认为资源为同一资源，断点续传才会生效
     * 注：
     * 同一资源的数据必须完全相同，否则上传可能会出现异常
     *
     * @return 资源修改时间
     */
    String getId();

    /**
     * 是否有效
     * @return 是否有效
     */
    boolean isValid();

    /**
     * 是否可以重新加载文件信息，也即是否可以重新读取信息
     * @return return
     */
    boolean couldReloadInfo();

    /**
     * 重新加载文件信息，以便于重新读取
     *
     * @return 重新加载是否成功
     */
    boolean reloadInfo();

    /**
     * 获取资源文件名
     * @return 资源文件名
     */
    String getFileName();

    /**
     * 获取资源大小
     * @return 资源大小
     */
    long getFileSize();

    /**
     * 读取数据
     * @param dataSize 数据大小
     * @param dataOffset 数据偏移量
     * @return 数据
     * @throws IOException 异常
     */
    byte[] readData(int dataSize, long dataOffset) throws IOException;

    /**
     * 关闭流
     */
    void close();
}
