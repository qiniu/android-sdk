package com.qiniu.auth;

import android.os.AsyncTask;
import org.apache.http.HttpEntity;
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
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class Client {
	
	protected HttpClient mClient;
	
	public Client(HttpClient client) {
		mClient = client;
	}

	public void call(String url, CallRet ret) {
		HttpPost httppost = new HttpPost(url);
		execute(httppost, ret);
	}

	public void call(String url, String contentType, HttpEntity entity, CallRet ret) {
		HttpPost httppost = new HttpPost(url);
		httppost.setEntity(entity);

		if (contentType != null) {
			httppost.setHeader("Content-Type", contentType);
		}
		execute(httppost, ret);
	}

	protected void execute(HttpPost httpPost, CallRet ret) {
		new ClientExecuter().execute(httpPost, ret);
	}

	protected HttpResponse roundtrip(HttpPost httpPost) throws IOException {
		return mClient.execute(httpPost);
	}

	class ClientExecuter extends AsyncTask<Object, Object, Object> {
		HttpPost httpPost;
		CallRet ret;
		
		@Override
		protected Object doInBackground(Object... objects) {
			httpPost = (HttpPost) objects[0];
			ret = (CallRet) objects[1];
			String errMsg = "";
			HttpResponse resp = null;
			byte[] data = new byte[]{};

			try {
				resp = roundtrip(httpPost);
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (resp.getHeaders("X-Log").length > 0) {
				errMsg = resp.getHeaders("X-Log")[0].getValue();
			}

			int statusCode = resp.getStatusLine().getStatusCode();

			if (statusCode / 100 != 2) {
				return new Exception(errMsg);
			}

			try {
				data = EntityUtils.toByteArray(resp.getEntity());
			} catch (IOException e) {
				e.printStackTrace();
			}

			return data;
		}

		@Override
		protected void onPostExecute(Object o) {
			if (o instanceof Exception) {
				ret.onFailure((Exception) o);
				return;
			}

			ret.onSuccess((byte[]) o);
		}
	};

	public static Client defaultClient() {
		return new Client(getMultithreadClient());
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
