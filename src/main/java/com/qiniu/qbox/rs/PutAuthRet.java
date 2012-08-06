package com.qiniu.qbox.rs;

import org.json.JSONException;
import org.json.JSONObject;

import com.qiniu.qbox.auth.CallRet;

public class PutAuthRet extends CallRet {
	private int expires;
	private String url;
	
	public PutAuthRet(CallRet ret) {
		super(ret);
		
		if (this.response != null) {
			try {
				unmarshal(ret.getResponse());
			} catch (Exception e) {
				this.exception = e;
			}
		}
	}
	
	public void unmarshal(String json) throws JSONException {
		
		JSONObject jsonObject = new JSONObject(json);
		
		if (jsonObject.has("expiresIn")) {
			this.expires = (Integer)jsonObject.get("expiresIn");
		}
		if (jsonObject.has("url")) {
			this.url = (String)jsonObject.get("url");
		}
	}
	
	public int getExpiresIn() {
		return this.expires;
	}
	
	public String getUrl() {
		return this.url;
	}
}
