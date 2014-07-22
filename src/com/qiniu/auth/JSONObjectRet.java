package com.qiniu.auth;

import org.json.JSONException;
import org.json.JSONObject;

import com.qiniu.utils.QiniuException;

public abstract class JSONObjectRet extends CallRet {
	public JSONObjectRet(){}
	protected int mIdx;
	public JSONObjectRet(int idx) { mIdx = idx; }
	@Override
	public void onSuccess(byte[] body) {
		if (body == null) {
			onSuccess(new JSONObject());
			return;
		}
		try {
			JSONObject obj = new JSONObject(new String(body));
			onSuccess(obj);
		} catch (JSONException e) {
			e.printStackTrace();
			onFailure(new QiniuException(QiniuException.JSON, new String(body), e));
		}
	}

	public abstract void onSuccess(JSONObject obj);
}
