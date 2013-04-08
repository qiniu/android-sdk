package com.qiniu.auth;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;

public class UpClient extends Client {
    private String mUpToken;

    public UpClient(String upToken) {
        this(upToken, getMultithreadClient());
    }

    public UpClient(String upToken, HttpClient client) {
		super(client);
        mUpToken = upToken;
    }

	@Override
	protected HttpResponse roundtrip(HttpPost httpPost) throws IOException {
		httpPost.setHeader("Authorization", "UpToken " + mUpToken);
		return super.roundtrip(httpPost);
	}

	public static UpClient defaultClient(String uptoken) {
		return new UpClient(uptoken);
	}

	public static HttpClient getMultithreadClient() {
		HttpParams params = new BasicHttpParams();
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		ClientConnectionManager cm = new ThreadSafeClientConnManager(params, registry);
		HttpClient client = new DefaultHttpClient(cm, params);
		return client;
	}
}
