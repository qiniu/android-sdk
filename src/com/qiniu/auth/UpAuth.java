package com.qiniu.auth;

import android.os.AsyncTask;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class UpAuth {
    private String mUpToken;
    private HttpClient mClient;

    public UpAuth(String upToken) {
        this(upToken, new DefaultHttpClient());
    }

    public UpAuth(String upToken, HttpClient client) {
        mUpToken = upToken;
        mClient = client;
    }

    public void Call(String url, CallRet ret) {
        HttpPost httppost = new HttpPost(url);
        execute(httppost, ret);
    }

    public void Call(String url, String contentType, HttpEntity entity, CallRet ret) {
        HttpPost httppost = new HttpPost(url);
        httppost.setHeader("Content-Type", contentType);
        httppost.setEntity(entity);
		execute(httppost, ret);
    }

	private void execute(HttpPost httpPost, CallRet ret) {
		httpPost.setHeader("Authorization", "UpToken " + mUpToken);
		asyncCall.execute(httpPost, ret);
	}

	private AsyncTask<Object, Object, Object> asyncCall = new AsyncTask<Object, Object, Object>() {
		HttpPost httpPost;
		CallRet ret;
		@Override
		protected Object doInBackground(Object... objects) {
			httpPost = (HttpPost) objects[0];
			ret = (CallRet) objects[1];
			try {
				HttpResponse resp = mClient.execute(httpPost);
				byte[] data = EntityUtils.toByteArray(resp.getEntity());
				if (resp.getStatusLine().getStatusCode() / 100 != 2) {
					try {
						JSONObject obj = new JSONObject(new String(data));
						return new Exception(obj.getString("error"));
					} catch (JSONException e) {

					}
					return new Exception(new String(data));
				}

				return data;
			} catch (IOException e) {
				return e;
			}
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
}
