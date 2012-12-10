package com.qiniu.qbox.up;

import org.json.JSONException;
import org.json.JSONObject;

import com.qiniu.qbox.auth.CallRet;

public class PutFileRet extends CallRet {

	private String hash;
	
	public PutFileRet(CallRet ret) {
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
		
		if (jsonObject.has("hash")) {
			this.hash = jsonObject.getString("hash");
		}
	}

	public String getHash() {
		return this.hash;
	}
}
