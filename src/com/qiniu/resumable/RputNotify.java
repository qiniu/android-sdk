package com.qiniu.resumable;

public class RputNotify {
	public synchronized void onNotify(int blkIdx, int blkSize, BlkputRet ret){}
	public synchronized void onError(int blkIdx, int blkSize, Exception ex){}
}
