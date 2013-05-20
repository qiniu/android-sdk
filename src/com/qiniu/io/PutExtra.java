package com.qiniu.io;

public class PutExtra {
    public String callbackParams; // 当 uptoken 指定了 CallbackUrl，则 CallbackParams 必须非空
	public String bucket;         // 仓库名
    public String mimeType;       // 可选。用户自定义 Meta，不能超过 256 字节
    public String customMeta;     // 可选。在 uptoken 没有指定 DetectMime 时，用户客户端可自己指定 MimeType
	public PutExtra(String bucket) {
		this.bucket = bucket;
	}
}
