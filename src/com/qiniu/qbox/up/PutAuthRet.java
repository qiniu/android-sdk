package com.qiniu.qbox.up;

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
			this.expires = jsonObject.getInt("expiresIn");
		}
		if (jsonObject.has("url")) {
			this.url = jsonObject.getString("url");
		}
	}

	public int getExpiresIn() {
		return this.expires;
	}

	public String getUrl() {
		return this.url;
	}
}
