package com.qiniu.android.utils;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.utils.Crc32;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CrcTest {

    @Test
    public void testCrc() {
        byte[] data = "Hello, World!".getBytes();
        long result = Crc32.bytes(data);
        assertEquals(3964322768L, result);
    }
}
