package com.qiniu.rs;

import com.qiniu.resumableio.SliceUploadTask.Block;

public abstract class CallBack {
	
	public abstract void onProcess(long current, long total);
	public abstract void onSuccess(UploadCallRet ret);
	public abstract void onFailure(CallRet ret);
	public void onBlockSuccess(Block blk){}
	
}
