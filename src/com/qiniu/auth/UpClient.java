package com.qiniu.auth;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;

import java.io.IOException;

public class UpClient extends Client {
	private String mUpToken;

	public UpClient(HttpClient client) {
		super(client);
	}

	public void updateToken(String token) {
		mUpToken = token;
	}

	@Override
	protected HttpResponse roundtrip(HttpPost httpPost) throws IOException {
		if (mUpToken != null) {
			httpPost.setHeader("Authorization", "UpToken " + mUpToken);
		}
		return super.roundtrip(httpPost);
	}

	public static UpClient defaultClient() {
		return new UpClient(getMultithreadClient());
	}

}
