package com.qiniu.android.collect;

import com.qiniu.android.utils.ContextGetter;

/**
 * Created by Simon on 11/22/16.
 */
public class Config {
    /**
     * 是否记录上传信息
     */
    public static boolean isRecord = true;

    /**
     * 上传信息记录文件保存的目录. 绝对路径
     * 默认使用 ContextGetter.applicationContext().getCacheDir()
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
     * 记录上传信息文件最大值tem.out
     */
    public static int maxRecordFileSize = 4 * 512 * 1024;

    /**
     * 记录文件大于 uploadThreshold 后才可能触发上传
     */
    public static int uploadThreshold = 128 * 1024;


    /**
     * 每次上传最小时间间隔.单位:分钟
     */
    public static int minInteval = 3;

    /**
     * 上传方式,默认 false, 不上传.
     * <p/>
     * 为 true 时上传方式:
     * 增加一条上传记录时触发,
     * isRecord 为 true,
     * 且 单个记录文件大小 大于 uploadThreshold
     * 且 距上次上传时间大于 minInteval .
     * <p/>
     * 单个记录文件大小 大于 maxRecordFileSize, 则暂停记录信息
     */
    public static boolean isUpload = true;

    /**
     * 上传信息收集文件的地址
     * */
    public static String serverURL = "http://115.236.16.114:16536/log";
}
