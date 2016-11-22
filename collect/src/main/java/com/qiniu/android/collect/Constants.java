package com.qiniu.android.collect;

public final class Constants {
    public static final String VERSION = "0.1.1";

    public static final String UTF_8 = "utf-8";

    /**
     * 是否记录上传信息
     * */
    public static boolean isRecord = false;

    /**
     * 上传信息记录文件保存的目录. 绝对路径
     * */
    public static String recordDir = null;

    /**
     *  记录上传信息文件最大值,3M
     *  */
    public static int maxRecordFileSize = 6 * 512 * 1024;

    /**
     * 记录文件大于 uploadThreshold 后才可能触发上传
     * */
    public static int uploadThreshold = 512 * 1024;


    /**
     * 上传方式: 记录文件满时触发
     * isRecord 为 true,
     * 且 单个记录文件大小 大于 uploadThreshold
     * 且 距上次上传时间大于 minInteval .
     *
     * 单个记录文件大小 大于 maxRecordFileSize, 则暂停记录信息
     * */
    public static int WhileRecordFill = 1;

    /**
     * 每次上传最小时间间隔.单位:分钟
     * */
    public static int minInteval = 5;

    /**
     * 上传方式,默认 false, 不上传.
     * */
    public static boolean isUpload = false;


}
