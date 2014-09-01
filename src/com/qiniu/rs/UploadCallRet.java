package com.qiniu.rs;

import org.json.JSONException;
import org.json.JSONObject;

import com.qiniu.rs.CallRet;

public class UploadCallRet extends CallRet {
	protected String hash;
	protected String key;

	public UploadCallRet(CallRet ret) {
		super(ret);
	}

	public UploadCallRet(int statusCode, String reqid, String response) {
		super(statusCode, reqid, response);
	}
	
	public UploadCallRet(int statusCode, Exception e) {
		this(statusCode, "", e);
	}
	
	public UploadCallRet(int statusCode, String reqid, Exception e) {
		super(statusCode, reqid, e);
	}

	@Override
	protected void unmarshal() throws JSONException{
		JSONObject jsonObject = new JSONObject(this.getResponse());
		hash = jsonObject.optString("hash", null);
		key = jsonObject.optString("key", null);
	}

	public String getHash() {
		return hash;
	}

	public String getKey() {
		return key;
	}
	
}
