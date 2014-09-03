package com.qiniu.utils;

import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRoute;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import com.qiniu.conf.Conf;

public class Http {
	private static HttpClient httpClient;

	public static HttpClient getHttpClient() {
		if (httpClient == null) {
			httpClient = buildHttpClient();
		}
		return httpClient;
	}
	
	private static HttpClient buildHttpClient() {
		HttpParams httpParams = new BasicHttpParams();
		ConnManagerParams.setMaxTotalConnections(httpParams, 10);
		ConnPerRoute connPerRoute = new ConnPerRouteBean(3);
		ConnManagerParams.setMaxConnectionsPerRoute(httpParams, connPerRoute);
		HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);

		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));
		registry.register(new Scheme("https", SSLSocketFactory
				.getSocketFactory(), 443));
		ClientConnectionManager cm = new ThreadSafeClientConnManager(
				httpParams, registry);

		HttpClient httpClient = new DefaultHttpClient(cm, httpParams);
		
		httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, Conf.SO_TIMEOUT);
		httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, Conf.CONNECTION_TIMEOUT);
		
		return httpClient;
	}
	
}
