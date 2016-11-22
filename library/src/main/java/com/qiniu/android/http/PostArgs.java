package com.qiniu.android.http;

import com.qiniu.android.utils.StringMap;

import java.io.File;

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
    public StringMap params;
    /**
     * 上传文件名
     */
    public String fileName;
    /**
     * 上传文件或数据的MimeType
     */
    public String mimeType;

}
