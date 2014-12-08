package com.qiniu.android.storage;

import java.util.HashMap;
import java.util.Map;

/**
 *  定义数据或文件上传时的可选项
 */
public final class UploadOptions {

    /**
     *  扩展参数，以<code>x:</code>开头的用户自定义参数
     */
    Map<String, String> params;

    /**
     *  指定上传文件的MimeType
     */
    String mimeType;

    /**
     *  启用上传内容crc32校验
     */
    boolean checkCrc;

    /**
     *  上传内容进度处理
     */
    UpProgressHandler progressHandler;

    /**
     *  取消上传信号
     */
    UpCancellationSignal cancellationSignal;

    public UploadOptions(Map<String, String> params, String mimeType, boolean checkCrc,
                         UpProgressHandler progressHandler, UpCancellationSignal cancellationSignal) {
        this.params = filterParam(params);
        this.mimeType = mimeType;
        this.checkCrc = checkCrc;
        this.progressHandler = progressHandler;
        this.cancellationSignal = cancellationSignal;
    }

    /**
     *  过滤用户自定义参数，只有参数名以<code>x:</code>开头的参数才会被使用
     *
     *  @param params 待过滤的用户自定义参数
     *
     *  @return 过滤后的用户自定义参数
     */
    private static Map<String, String> filterParam(Map<String, String> params) {
        Map<String, String> ret = new HashMap<String, String>();
        if (params == null) {
            return ret;
        }

        for (Map.Entry<String, String> i : params.entrySet()) {
            if (i.getKey().startsWith("x:")) {
                ret.put(i.getKey(), i.getValue());
            }
        }
        return ret;
    }
}
