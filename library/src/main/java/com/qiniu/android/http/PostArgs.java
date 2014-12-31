package com.qiniu.android.http;

import java.io.File;
import java.util.Map;

/**
 * 定义请求参数列表
 */
public final class PostArgs {
    /**
     * 上传的数据
     */
    public byte[] data;
    /**
     * 上传的文件
     */
    public File file;
    /**
     * 请求参数
     */
    public Map<String, String> params;
    /**
     * 上传文件名
     */
    public String fileName;
    /**
     * 上传文件或数据的MimeType
     */
    public String mimeType;
}
