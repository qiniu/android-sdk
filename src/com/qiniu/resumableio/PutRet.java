package com.qiniu.resumableio;

import org.json.JSONObject;

public class PutRet {
    public String ctx;
    public String host;
    public int crc32;
    public String checksum;
    public int offset = 0;
    public boolean isInvalid() {
        return ctx == null || offset == 0;
    }
    public void parse(JSONObject obj) {
        ctx = obj.optString("ctx", "");
        host = obj.optString("host", "");
        crc32 = obj.optInt("crc32", 0);
        checksum = obj.optString("checksum", "");
        offset = obj.optInt("offset", 0);
    }
}
