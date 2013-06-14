package com.qiniu.auth;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;

import java.io.IOException;

public class UpClient extends Client {
    private String mUpToken;

    public UpClient(String upToken, HttpClient client) {
		super(client);
        mUpToken = upToken;
    }

	public void updateToken(String token) {
		mUpToken = token;
	}

	@Override
	protected HttpResponse roundtrip(HttpPost httpPost) throws IOException {
		httpPost.setHeader("Authorization", "UpToken " + mUpToken);
		return super.roundtrip(httpPost);
	}

	public static UpClient defaultClient(String uptoken) {
		return new UpClient(uptoken, getMultithreadClient());
	}

}
