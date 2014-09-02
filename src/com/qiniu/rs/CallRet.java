package com.qiniu.rs;


public class CallRet {
	private final int statusCode;
	private final String reqId;
	private final String response;
	private Exception exception;
	
	/**
	 * 子类必须实现此构造函数
	 * @param ret
	 */
	public CallRet(CallRet ret){
		this.statusCode = ret.statusCode;
		this.reqId = ret.reqId;
		this.response = ret.response;
		this.exception = ret.exception;
		doUnmarshal();
	}
	
	public CallRet(int statusCode, String reqId, String responseBody) {
		this.statusCode = statusCode;
		this.reqId = reqId;
		this.response = responseBody;
		doUnmarshal();
	}
	
	public CallRet(int errorCode, String reqId, Exception e) {
		this.statusCode = errorCode;
		this.reqId = reqId;
		this.response = "";
		exception = e;
		doUnmarshal();
	}
	
	private void doUnmarshal() {
		if (this.exception != null || this.response == null
				|| !this.response.trim().startsWith("{")) {
			return;
		}
		try {
			unmarshal();
		} catch (Exception e) {
			if (this.exception == null) {
				this.exception = e;
			}
		}
	}

	protected void unmarshal() throws Exception {
		
	}
	
	public boolean isOk(){
		return this.statusCode / 100 == 2;
	}
	
	public int getStatusCode() {
		return statusCode;
	}
	public String getReqId() {
		return reqId;
	}
	public String getResponse() {
		return response;
	}
	public Exception getException() {
		return exception;
	}

	public String toString(){
		String s = "statusCode: " + statusCode + ", reqId: " + reqId + ", response: " + response;
		if(exception != null){
			s += ", ex: "+ exception.toString();
		}
		return s;
	}
	
}
