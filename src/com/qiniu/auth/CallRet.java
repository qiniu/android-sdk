package com.qiniu.auth;

public abstract class CallRet {
	public abstract void onSuccess(byte[] body);
	public abstract void onFailure(Exception ex);
	public void onSuccess(){}
}