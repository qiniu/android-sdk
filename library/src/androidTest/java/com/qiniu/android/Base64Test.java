package com.qiniu.android;

import android.test.AndroidTestCase;

import com.qiniu.android.utils.UrlSafeBase64;

import junit.framework.Assert;

import java.io.UnsupportedEncodingException;

public class Base64Test extends AndroidTestCase {
    public void testEncode() throws UnsupportedEncodingException {
        String data = "你好/+=";
        String result = UrlSafeBase64.encodeToString(data);
        Assert.assertEquals("5L2g5aW9Lys9", result);
    }
}
