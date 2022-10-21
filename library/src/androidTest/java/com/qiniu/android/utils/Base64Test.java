package com.qiniu.android.utils;

import com.qiniu.android.BaseTest;

import org.junit.Test;

import java.io.UnsupportedEncodingException;


public class Base64Test extends BaseTest {

    @Test
    public void testEncode() throws UnsupportedEncodingException {
        String data = "你好/+=";
        String result = UrlSafeBase64.encodeToString(data);
        assertEquals("5L2g5aW9Lys9", result);
    }
}
