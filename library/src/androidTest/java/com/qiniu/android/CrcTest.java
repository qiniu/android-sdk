package com.qiniu.android;

import android.test.AndroidTestCase;

import com.qiniu.android.utils.Crc32;

import junit.framework.Assert;

public class CrcTest extends AndroidTestCase {
    public void testCrc() {
        byte[] data = "Hello, World!".getBytes();
        long result = Crc32.bytes(data);
        Assert.assertEquals(3964322768L, result);
    }
}
