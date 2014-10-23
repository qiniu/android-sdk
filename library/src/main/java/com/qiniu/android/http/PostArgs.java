package com.qiniu.android.http;

import java.io.File;
import java.util.Map;


public final class PostArgs {
    public byte[] data;
    public File file;
    public Map<String, String> params;
    public String fileName;
    public String mimeType;
}
