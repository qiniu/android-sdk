package com.qiniu.rs;

import com.qiniu.resumableio.SliceUploadTask.Block;
import com.qiniu.utils.QiniuException;

public abstract class CallBack {
	
	public abstract void onProcess(long current, long total);
	public abstract void onSuccess(UploadCallRet ret);
	public abstract void onFailure(CallRet ret, QiniuException ex);
	public void onBlockSuccess(Block blk){}
	
}
