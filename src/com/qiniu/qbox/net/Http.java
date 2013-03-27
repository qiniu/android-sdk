package com.qiniu.qbox.net;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

public class Http {

	private static HttpClient client;

	public static void setClient(HttpClient c) {
		client = c;
	}

	public static HttpClient getClient() {
		if (client == null) {
			client = makeDefaultClient();
		}
		return client;
	}

	private static HttpClient makeDefaultClient() {

		HttpParams params = new BasicHttpParams();
		ConnManagerParams.setTimeout(params, 1000);
		// Increase default max connection per route to 20
		ConnManagerParams.setMaxConnectionsPerRoute(params,
				new ConnPerRouteBean(20));

		// Increase max total connection to 200
		ConnManagerParams.setMaxTotalConnections(params, 200);

		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));

		schemeRegistry.register(new Scheme("https", SSLSocketFactory
				.getSocketFactory(), 443));

		ClientConnectionManager manager = new ThreadSafeClientConnManager(
				params, schemeRegistry);
		
		return new DefaultHttpClient(manager, params);
	}
}