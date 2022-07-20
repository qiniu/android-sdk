package com.qiniu.android.collect;

import com.qiniu.android.common.Config;
import com.qiniu.android.utils.Utils;

public class ReportConfig {

    /**
     * 是否开启sdk上传信息搜集  默认为YES
     */
    public boolean isReportEnable;

    /**
     * 每次上传时间间隔  单位：分钟  默认为0.5分钟
     */
    public double interval;

    /**
     *  记录文件大于 uploadThreshold 会触发上传，单位：字节  默认为16 * 1024
     */
    public long uploadThreshold;

    /**
     * 记录文件最大值  要大于 uploadThreshold  单位：字节  默认为20 * 1024 * 1024
     */
    public long maxRecordFileSize;

    /**
     * 记录文件所在文件夹目录
     */
    public final String recordDirectory;

    /**
     * 信息上报服务器地址
     */
    public final String serverURL;

    /**
     * 信息上报请求超时时间  单位：秒  默认为10秒
     */
    public int timeoutInterval;

    private static ReportConfig instance = new ReportConfig();

    private ReportConfig(){
        this.isReportEnable = Config.isRecord;
        this.interval = Config.interval;
        this.serverURL = Config.upLogURL;
        if (Config.recordDir != null) {
            this.recordDirectory = Config.recordDir;
        } else {
            this.recordDirectory = Utils.sdkDirectory() + "/report";
        }
        this.maxRecordFileSize = Config.maxRecordFileSize;
        this.uploadThreshold = Config.uploadThreshold;
        this.timeoutInterval = 10;
    }

    public static ReportConfig getInstance(){
        return instance;
    }
}
