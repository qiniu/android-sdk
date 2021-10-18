package com.qiniu.android;

import com.qiniu.android.utils.UrlSafeBase64;
import java.io.UnsupportedEncodingException;

public class Base64Test extends BaseTest {
    public void testEncode() throws UnsupportedEncodingException {
        String data = "你好/+=";
        String result = UrlSafeBase64.encodeToString(data);
        assertEquals("5L2g5aW9Lys9", result);
    }
}
