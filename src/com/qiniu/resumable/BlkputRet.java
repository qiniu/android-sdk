package com.qiniu.resumable;

import org.json.JSONException;
import org.json.JSONObject;

public class BlkputRet {
	public static BlkputRet parse(JSONObject ret) {
		try {
			String ctx = ret.getString("ctx");
			String checksum = ret.getString("checksum");
			long crc32 = ret.getLong("crc32");
			int offset = ret.getInt("offset");
			String host = ret.getString("host");
			return new BlkputRet(ctx, checksum, crc32, offset, host);
		} catch (JSONException e) {
			return null;
		}
	}

	public String ctx;
	public String checksum;
	public long crc32;
	public int offset;
	public String host;

	public BlkputRet(String ctx, String checksum, long crc32, int offset, String host) {
		this.ctx = ctx;
		this.checksum = checksum;
		this.crc32 = crc32;
		this.offset = offset;
		this.host = host;
	}

	public boolean checkCrc32(long crc32) {
		return crc32 == this.crc32;
	}
}
