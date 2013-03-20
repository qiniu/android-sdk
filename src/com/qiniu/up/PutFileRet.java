package com.qiniu.up;

import com.qiniu.auth.CallRet;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class PutFileRet extends CallRet {
	@Override
	public final void onSuccess(byte[] body) {
		try {
			JSONObject obj = new JSONObject(new String(body));
			String hash = obj.getString("hash");
			onSuccess(hash);
		} catch (JSONException e) {
			onFailure(new Exception(new String(body)));
		}
	}

	public abstract void onSuccess(String hash);
}
