package com.qiniu.auth;

import com.qiniu.utils.IOnProcess;

public abstract class CallRet implements IOnProcess {
	public void onInit(int flag){}
	public abstract void onSuccess(byte[] body);
	public abstract void onFailure(Exception ex);
	public void onProcess(long current, long total){}
	public void onPause(Object tag){}
}
