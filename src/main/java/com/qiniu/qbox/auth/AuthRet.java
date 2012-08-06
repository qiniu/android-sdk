package com.qiniu.qbox.auth;

import org.json.JSONException;
import org.json.JSONObject;

public class AuthRet extends CallRet {
	private String accessToken;
	private String refreshToken;
	
	public AuthRet(CallRet ret) {
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
		
		if (jsonObject.has("access_token")) {
			this.accessToken = (String) jsonObject.get("access_token");
		}
		if (jsonObject.has("refresh_token")) {
			this.refreshToken = (String) jsonObject.get("refresh_token");
		}
	}
	
	public String getAccessToken() {
		return accessToken;
	}
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}
	public String getRefreshToken() {
		return refreshToken;
	}
	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
}
