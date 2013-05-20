package com.qiniu.resumable;

public class RputExtra {
	public String callbackParams; // 当 uptoken 指定了 CallbackUrl，则 CallbackParams 必须非空
	public String bucket;
	public String customMeta; // 可选。用户自定义 Meta，不能超过 256 字节
	public String mimeType; // 可选。在 uptoken 没有指定 DetectMime 时，用户客户端可自己指定 MimeType
	public int chunkSize; // 可选。每次上传的Chunk大小
	public int tryTimes; // 可选。尝试次数
	public BlkputRet[] progresses; // 可选。上传进度
	public RputNotify notify; // 可选。进度提示（注意多个block是并行传输的）

	public RputExtra(String bucket) {
		this.bucket = bucket;
	}
}
