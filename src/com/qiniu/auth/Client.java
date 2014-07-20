package com.qiniu.auth;

import android.os.AsyncTask;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


import com.qiniu.conf.Conf;
import com.qiniu.utils.ICancel;
import com.qiniu.utils.QiniuException;

public class Client {

	protected HttpClient mClient;

	public Client(HttpClient client) {
		mClient = client;
	}

	public void close() {
		mClient.getConnectionManager().closeExpiredConnections();
		mClient.getConnectionManager().shutdown();
	}

	public static ClientExecutor get(String url, CallRet ret) {
		Client client = Client.defaultClient();
		return client.get(client.makeClientExecutor(), url, ret);
	}

	public ClientExecutor call(ClientExecutor client, String url, HttpEntity entity, CallRet ret) {
		Header header = entity.getContentType();
		String contentType = header == null ? "application/octet-stream" :  header.getValue();

		return call(client, url, contentType, entity, ret);
	}

	public ClientExecutor call(ClientExecutor client, String url, String contentType, HttpEntity entity, CallRet ret) {
		HttpPost httppost = new HttpPost(url);
		httppost.setEntity(entity);

		if (contentType != null) {
			httppost.setHeader("Content-Type", contentType);
		}
		return execute(client, httppost, ret);
	}

	public ClientExecutor get(ClientExecutor client, String url, CallRet ret) {
		return execute(client, new HttpGet(url), ret);
	}

	public ClientExecutor makeClientExecutor() {
		return new ClientExecutor();
	}

	protected ClientExecutor execute(ClientExecutor client, HttpRequestBase httpRequest, final CallRet ret) {
		client.setup(httpRequest, ret);
		client.execute();
		return client;
	}

	protected HttpResponse roundtrip(HttpRequestBase httpRequest) throws IOException {
		httpRequest.setHeader("User-Agent", Conf.getUserAgent());
		return mClient.execute(httpRequest);
	}

	public class ClientExecutor extends AsyncTask<Object, Object, Object> implements ICancel {
		HttpRequestBase mHttpRequest;
		CallRet mRet;
		boolean failed;
		public void setup(HttpRequestBase httpRequest, CallRet ret) {
			mHttpRequest = httpRequest;
			mRet = ret;
		}
		public void upload(long current, long total) {
			publishProgress(current, total);
		}

		@Override
		protected Object doInBackground(Object... objects) {
			try {
				HttpResponse resp = roundtrip(mHttpRequest);
				int statusCode = resp.getStatusLine().getStatusCode();
				String phrase = resp.getStatusLine().getReasonPhrase();

				Header h = resp.getFirstHeader("X-Log");
				String xl = h == null ? "":h.getValue();

				h = resp.getFirstHeader("X-Reqid");
				String reqId = h == null ? "":h.getValue();

				if (statusCode == 401) {
					return new QiniuException(401, reqId, phrase); // android 2.3 will not response
				}

				byte[] data = EntityUtils.toByteArray(resp.getEntity());
				if (statusCode / 100 == 2) {
					return data;
				}

				if (data.length > 0) {
					return new QiniuException(statusCode, reqId, new String(data));
				}
				if (xl.length() > 0) {
					return new QiniuException(statusCode, reqId, xl);
				}
				return new QiniuException(statusCode, reqId, phrase);
			} catch (IOException e) {
				e.printStackTrace();
				return new QiniuException(QiniuException.IO, "net IOException", e);
			}
		}

		@Override
		protected void onProgressUpdate(Object... values) {
			if (failed){
				return;
			}
			if (values.length == 1 && values[0] instanceof QiniuException) {
				mRet.onFailure((QiniuException) values[0]);
				failed = true;
				return;
			}
			mRet.onProcess((Long) values[0], (Long) values[1]);
		}

		@Override
		protected void onPostExecute(Object o) {
			if (failed) {
				return;
			}
			if (o instanceof QiniuException) {
				mRet.onFailure((QiniuException) o);
				return;
			}
			mRet.onSuccess((byte[]) o);
		}

		public void onFailure(QiniuException ex) {
			publishProgress(ex);
			cancel(true);
		}
	};

	public static Client defaultClient() {
		return new Client(getMultithreadClient());
	}

	public static HttpClient getMultithreadClient() {
		HttpClient client = new DefaultHttpClient();
		ClientConnectionManager mgr = client.getConnectionManager();
		HttpParams params = client.getParams();
		client = new DefaultHttpClient(new ThreadSafeClientConnManager(params, mgr.getSchemeRegistry()), params);
		return client;
	}
}
