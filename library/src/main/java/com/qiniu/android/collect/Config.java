package com.qiniu.android.collect;

import com.qiniu.android.utils.ContextGetter;

/**
 * Created by Simon on 11/22/16.
 */
public class Config {
    /**
     * 是否记录上传状态信息。 true 表示记录，false 表示不记录。
     *
     * 记录上传信息条件：
     * isRecord 为 true,
     * 记录文件大小 小于 maxRecordFileSize .
     *
     * 记录文件大小 大于 maxRecordFileSize 时, 则暂停记录信息。
     */
    public static boolean isRecord = true;

    /**
     * 是否上传记录的上传状态信息。true 表示上传，false 表示不上传。
     *
     * 上传条件:
     * 增加一条上传记录时触发,
     * isRecord 为 true, isUpload 为 true,
     * 且 记录文件大小 大于 uploadThreshold,
     * 且 距上次上传时间大于 minInteval .
     *
     * 上传成功后，清空记录文件文件
     */
    public static boolean isUpload = true;

    /**
     * 上传信息记录文件保存的目录， 绝对路径。
     * 默认使用当前应用的缓存目录： getCacheDir()
     */
    public static String recordDir = null;
    static {
        try {
            recordDir = ContextGetter.applicationContext().getCacheDir().getAbsolutePath();
        } catch (Throwable e) {
            e.fillInStackTrace();
        }
    }

    /**
     * 记录上传信息文件最大值，单位：字节。
     *
     * 记录文件大于此值后暂停记录上传信息。
     */
    public static int maxRecordFileSize = 1024 * 1024;

    /**
     * 记录文件大于 uploadThreshold 后才可能触发上传，单位：字节。
     *
     * 以 200,CwIAAF4znMnpno0U,up.qiniu.com,183.131.7.18,80,383.0,1481014578,262144 为例，
     * 100 条，约 7400Byte ；50 条，约 3700； 1024 约 13.8 条
     *
     * chunkSize = 256 * 1024；putThreshold = 512 * 1024
     * 分片上传， 1M，最好情况下 5 个请求；10M，最好情况下 41 个请求
     *
     * 可依据客户上传频率、文件大小做调整
     */
    public static int uploadThreshold = 4 * 1024;

    /**
     * 每次上传最小时间间隔.单位:分钟
     */
    public static int interval = 5;


    /**
     * 上传信息收集文件的地址
     * */
    public static String serverURL = "https://api.qiniu.com/uplog";
}
