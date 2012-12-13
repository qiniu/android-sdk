package com.qiniu.qbox.up;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import com.qiniu.qbox.auth.CallRet;

public class GetRet extends CallRet {
	private String hash;
	private long fsize;
	private String mimeType;
	private String url;

	public GetRet(CallRet ret) {
		super(ret);

		if (ret.ok() && ret.getResponse() != null) {
			try {
				unmarshal(ret.getResponse());
			} catch (Exception e) {
				this.exception = e;
			}
		}
	}

	public String getHash() {
		return this.hash;
	}

	public long getFsize() {
		return this.fsize;
	}

	public String getUrl() {
		return this.url;
	}

	public String getMimeType() {
		return this.mimeType;
	}

	@SuppressLint("UseValueOf")
	public void unmarshal(String json) throws JSONException {

		JSONObject jsonObject = new JSONObject(json);

		if (jsonObject.has("hash") && jsonObject.has("fsize")
				&& jsonObject.has("mimeType") && jsonObject.has("url")) {
			this.hash = jsonObject.getString("hash");
			Object fsizeObject = jsonObject.get("fsize");
			if (fsizeObject instanceof Long) {
				this.fsize = (Long) fsizeObject;
			} else if (fsizeObject instanceof Integer) {
				this.fsize = new Long((int) (Integer) fsizeObject);
			}
			this.mimeType = jsonObject.getString("mimeType");
			this.url = jsonObject.getString("url");
		}
	}

}