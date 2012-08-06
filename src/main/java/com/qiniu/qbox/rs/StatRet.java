package com.qiniu.qbox.rs;

import org.json.JSONException;
import org.json.JSONObject;

import com.qiniu.qbox.auth.CallRet;

public class StatRet extends CallRet {
	private String hash;
	private long fsize;
	private long putTime;
	private String mimeType;

	public StatRet(CallRet ret) {
		super(ret);
		
		if (ret.ok() && ret.getResponse() != null) {
			try {
				unmarshal(ret.getResponse());
			} catch (Exception e) {
				this.exception = e;
			}
		}
	}

	public void unmarshal(String json) throws JSONException {
		JSONObject jsonObject = new JSONObject(json);
		
		this.hash = (String)jsonObject.get("hash");
		Object fsizeObject = jsonObject.get("fsize");
		if (fsizeObject instanceof Long) {
			this.fsize = (Long)fsizeObject;
		} else if (fsizeObject instanceof Integer) {
			this.fsize = new Long((int)(Integer)fsizeObject);
		}
		Object putTimeObject = jsonObject.get("putTime");
		if (putTimeObject instanceof Long) {
			this.putTime = (Long)putTimeObject;
		} else if (putTimeObject instanceof Integer) {
			this.putTime = new Long((int)(Integer)putTimeObject);
		}
		this.mimeType = (String)jsonObject.get("mimeType");
	}
	
	public String getHash() {
		return this.hash;
	}
	
	public long getFsize() {
		return this.fsize;
	}

	public long getPutTime() {
		return this.putTime;
	}

	public String getMimeType() {
		return this.mimeType;
	}
}