package com.qiniu.resumableio;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PutExtra {
	public Map<String, String> params;
	public PutRet[] processes;
	public String mimeType;
	public INotify notify;

	long totalSize;

	public PutExtra() {}
	public PutExtra(JSONObject obj) {
		mimeType = obj.optString("mimeType", "");
		JSONArray procs = obj.optJSONArray("processes");
		processes = new PutRet[procs.length()];
		for (int i=0; i<procs.length(); i++) {
			processes[i] = new PutRet(procs.optJSONObject(i));
		}
		params = new HashMap<String, String>();
		JSONObject paramsJson = obj.optJSONObject("params");
		for (Iterator<?> iter = paramsJson.keys(); iter.hasNext();) {
			String key = (String) iter.next();
			params.put(key, paramsJson.optString(key));
		}
	}

	public boolean isFinishAll() {
		if (totalSize <= 0) return false;
		long currentSize = 0;
		for (PutRet pr: processes) {
			currentSize += pr.offset;
		}
		return currentSize >= totalSize;
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		JSONArray process = new JSONArray();
		for (PutRet p: processes) {
			process.put(p.toJSON());
		}
		json.put("processes", process);
		json.put("mimeType", mimeType);
		if (params != null) json.put("params", new JSONObject(params));
		return json;
	}
	public interface INotify {
		public void onSuccessUpload(PutExtra ex);
	}
}
