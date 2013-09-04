package com.qiniu.auth;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class JSONObjectRet extends CallRet {
	public JSONObjectRet(){}
	protected int mIdx;
	public JSONObjectRet(int idx) { mIdx = idx; }
	@Override
	public void onSuccess(byte[] body) {
		if (body == null) {
			onSuccess(new JSONObject());
		}
		try {
			JSONObject obj = new JSONObject(new String(body));
			onSuccess(obj);
		} catch (JSONException e) {
			e.printStackTrace();
			onFailure(new Exception(new String(body)));
		}
	}

	public abstract void onSuccess(JSONObject obj);
}
