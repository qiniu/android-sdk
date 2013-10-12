package com.qiniu.resumableio;

import org.json.JSONException;
import org.json.JSONObject;

public class PutRet {
	public String ctx;
	public String host;
	public long crc32;
	public String checksum;
	public int offset = 0;
	public boolean isInvalid() {
		return ctx == null || offset == 0;
	}
	public PutRet(){}
	public PutRet(JSONObject obj) {parse(obj);}
	public PutRet parse(JSONObject obj) {
		ctx = obj.optString("ctx", "");
		host = obj.optString("host", "");
		crc32 = Long.valueOf(obj.optString("crc32", "0"));
		checksum = obj.optString("checksum", "");
		offset = obj.optInt("offset", 0);
		return this;
	}
	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("crc32", crc32);
		json.put("checksum", checksum);
		json.put("offset", offset);
		json.put("host", host);
		json.put("ctx", ctx);
		return json;
	}
}
