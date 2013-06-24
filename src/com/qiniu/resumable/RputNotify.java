package com.qiniu.resumable;

public class RputNotify {
    private long total = 0;
    private long uploaded = 0;
    public final void setTotal(long total) {
        this.total = total;
    }
    public synchronized void onProcess(long uploaded, long total){}
	public synchronized void onNotify(int blkIdx, int blkSize, BlkputRet ret){
        uploaded += blkSize;
        onProcess(uploaded, total);
    }
	public synchronized void onError(int blkIdx, int blkSize, Exception ex){}
}
