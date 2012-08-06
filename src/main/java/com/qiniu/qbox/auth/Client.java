package com.qiniu.qbox.auth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public abstract class Client {

	public abstract void setAuth(HttpPost post);

	public CallRet call(String url) {
		HttpPost postMethod = new HttpPost(url);
		HttpClient client = new DefaultHttpClient();
		try {
			setAuth(postMethod);
			HttpResponse response = client.execute(postMethod);
			return handleResult(response);
		} catch (Exception e) {
			e.printStackTrace();
			return new CallRet(400, e);
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

	public CallRet call(String url, List<NameValuePair> nvps) {
		HttpPost postMethod = new HttpPost(url);
		HttpClient client = new DefaultHttpClient();
		try {
			StringEntity entity = new UrlEncodedFormEntity(nvps, "UTF-8");
			entity.setContentType("application/x-www-form-urlencoded");
			postMethod.setEntity(entity);

			setAuth(postMethod);
			HttpResponse response = client.execute(postMethod);

			return handleResult(response);
		} catch (Exception e) {
			e.printStackTrace();
			return new CallRet(400, e);
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

	public CallRet callWithBinary(String url, AbstractHttpEntity entity) {

		HttpPost postMethod = new HttpPost(url);

		postMethod.setEntity(entity);

		DefaultHttpClient client = new DefaultHttpClient();

		try {
			setAuth(postMethod);
			HttpResponse response = client.execute(postMethod);
			return handleResult(response);
		} catch (Exception e) {
			e.printStackTrace();
			return new CallRet(400, e);
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

	public CallRet callWithBinary(String url, String contentType, byte[] body, long bodyLength) {

		ByteArrayEntity entity = new ByteArrayEntity(body);

		if (contentType == null || contentType.isEmpty()) {
			contentType = "application/octet-stream";
		}
		entity.setContentType(contentType);

		return callWithBinary(url, entity);
	}

	private CallRet handleResult(HttpResponse response) {

		if (response == null || response.getStatusLine() == null) {
			return new CallRet(400, "No response");
		}

		String responseBody;
		try {
			responseBody = EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			e.printStackTrace();
			return new CallRet(400, e);
		}

		StatusLine status = response.getStatusLine();
		int statusCode = (status == null) ? 400 : status.getStatusCode();

		return new CallRet(statusCode, responseBody);
	}

	public static byte[] urlsafeEncodeBytes(byte[] src) {
		if (src.length % 3 == 0) {
			return encodeBase64Ex(src);//, false, true);
		}

		byte[] b = encodeBase64Ex(src);//, false, true);
		if (b.length % 4 == 0) {
			return b;
		}

		int pad = 4 - b.length % 4;
		byte[] b2 = new byte[b.length + pad];
		System.arraycopy(b, 0, b2, 0, b.length);
		b2[b.length] = '=';
		if (pad > 1) {
			b2[b.length+1] = '=';
		}
		return b2;
	}

	public static String urlsafeEncodeString(byte[] src) {
		return new String(urlsafeEncodeBytes(src));
	}

	public static String urlsafeEncode(String text) {
		return new String(urlsafeEncodeBytes(text.getBytes()));
	}

	private static byte[] encodeBase64Ex(byte[] src) {
		byte[] b64 = Base64.encodeBase64(src); // urlsafe version is not supported in version 1.4 or lower.

		for (int i = 0; i < b64.length; i++) {
			if (b64[i] == '/') {
				b64[i] = '_';
			} else if (b64[i] == '+') {
				b64[i] = '-';
			}
		}
		return b64;
	}

	@SuppressWarnings("unchecked")
	public static String encodeParams(Object params1) {
		if (params1 instanceof String) {
			return (String)params1;
		}
		if (params1 instanceof HashMap<?, ?>) {
			HashMap<String, String> params = (HashMap<String, String>)params1;
			ArrayList<NameValuePair> list = new ArrayList<NameValuePair>();
			for (Entry<String, String> entry : params.entrySet()) {
				list.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
			}
			return URLEncodedUtils.format(list, "UTF-8");
		}
		return null;
	}
}
