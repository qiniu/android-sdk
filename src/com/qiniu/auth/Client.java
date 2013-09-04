package com.qiniu.auth;

import android.os.AsyncTask;
import com.qiniu.conf.Conf;
import com.qiniu.utils.ICancel;
import org.apache.http.Header;
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
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class Client {

	protected HttpClient mClient;

	public Client(HttpClient client) {
		mClient = client;
	}

	public void call(ClientExecuter client, String url, HttpEntity entity, CallRet ret) {
		Header header = entity.getContentType();
		String contentType = "application/octet-stream";
		if (header != null) {
			contentType = header.getValue();
		}
		call(client, url, contentType, entity, ret);
	}

	public void call(ClientExecuter client, String url, String contentType, HttpEntity entity, CallRet ret) {
		HttpPost httppost = new HttpPost(url);
		httppost.setEntity(entity);

		if (contentType != null) {
			httppost.setHeader("Content-Type", contentType);
		}
		execute(client, httppost, ret);
	}

	public ClientExecuter makeClientExecuter() {
		return new ClientExecuter();
	}

	protected ClientExecuter execute(ClientExecuter client, HttpPost httpPost, final CallRet ret) {
		client.setup(httpPost, ret);
		client.execute();
		return client;
	}

	protected HttpResponse roundtrip(HttpPost httpPost) throws IOException {
		httpPost.setHeader("User-Agent", Conf.USER_AGENT);
		return mClient.execute(httpPost);
	}

	public class ClientExecuter extends AsyncTask<Object, Object, Object> implements ICancel {
		HttpPost mHttpPost;
		CallRet mRet;
		public void setup(HttpPost httpPost, CallRet ret) {
			mHttpPost = httpPost;
			mRet = ret;
		}
		public void upload(long current, long total) {
			publishProgress(current, total);
		}
		
		@Override
		protected Object doInBackground(Object... objects) {
			try {
				HttpResponse resp = roundtrip(mHttpPost);
				int statusCode = resp.getStatusLine().getStatusCode();
				if (statusCode == 401) { // android 2.3 will not response
					return new Exception(resp.getStatusLine().getReasonPhrase());
				}
				byte[] data = EntityUtils.toByteArray(resp.getEntity());

				if (statusCode / 100 != 2) {
					if (data.length == 0) {
						String xlog = resp.getFirstHeader("X-Log").getValue();
						if (xlog.length() > 0) {
							return new Exception(xlog);
						}
						return new Exception(resp.getStatusLine().getReasonPhrase());
					}
					return new Exception(new String(data));
				}
				return data;
			} catch (IOException e) {
				e.printStackTrace();
				return e;
			}
		}

		@Override
		protected void onProgressUpdate(Object... values) {
			mRet.onProcess((Long) values[0], (Long) values[1]);
		}

		@Override
		protected void onPostExecute(Object o) {
			if (o instanceof Exception) {
				mRet.onFailure((Exception) o);
				return;
			}
			mRet.onSuccess((byte[]) o);
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
