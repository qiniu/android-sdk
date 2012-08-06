package com.qiniu.qbox.auth;

import org.apache.http.client.methods.HttpPost;

public class UpTokenClient extends Client {

	private String token;
	
	public UpTokenClient(String token) {
		this.token = "UpToken " + token;
	}
	
	@Override
	public void setAuth(HttpPost post) {
		post.setHeader("Authorization", token);
	}

	public String getToken() {
		return token;
	}
}
