package com.qiniu.qbox.auth;


public class CallRet {
	public int statusCode;
	public String response;
	public Exception exception;
	
	public CallRet(int statusCode, String response) {
		this.statusCode = statusCode;
		this.response = response;
	}

	public CallRet(int statusCode, Exception e) {
		this.statusCode = statusCode;
		this.exception = e;
	}
	
	public CallRet(CallRet ret) {
		this.statusCode = ret.statusCode;
		this.exception = ret.exception;
		this.response = ret.response;
	}

	public int getStatusCode() {
		return this.statusCode;
	}
	
	public String getResponse() {
		return this.response;
	}
	
	public boolean ok() {
		return this.statusCode / 100 == 2;
	}
	
	public Exception getException() {
		return this.exception;
	}
	
	public String toString() {
		if (this.exception != null) {
			return this.exception.getMessage();
		}
		
		if (this.response != null) {
			return this.response;
		}
		
		return String.valueOf(this.statusCode);
	}
}
