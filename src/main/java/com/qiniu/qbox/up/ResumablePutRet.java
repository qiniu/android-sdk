package com.qiniu.qbox.up;

import org.json.JSONException;
import org.json.JSONObject;

import com.qiniu.qbox.auth.CallRet;

public class ResumablePutRet extends CallRet {
	private String ctx;
	private String checksum;
	private long crc32;
	
	public long getCrc32() {
		return crc32;
	}

	public void setCrc32(long crc32) {
		this.crc32 = crc32;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public String getCtx() {
		return ctx;
	}

	public void setCtx(String ctx) {
		this.ctx = ctx;
	}

	public ResumablePutRet(CallRet ret) {
		super(ret);
		
		if (ret.ok() && ret.getResponse() != null) {
			try {
				unmarshal(ret.getResponse());
			} catch (Exception e) {
				e.printStackTrace();
				ret.exception = e;
			} 
		}
	}
	
	protected void unmarshal(String json) throws JSONException {
		JSONObject jsonObject = new JSONObject(json);
		
		this.setCtx((String)jsonObject.get("ctx"));
		this.setChecksum((String)jsonObject.get("checksum"));
		
		Object crc32Object = jsonObject.get("crc32");
		if (crc32Object instanceof Long) {
			this.setCrc32((long)(Long)crc32Object);
		} else if (crc32Object instanceof Integer) {
			this.setCrc32((long)(int)(Integer)crc32Object);
		}
	}
}
