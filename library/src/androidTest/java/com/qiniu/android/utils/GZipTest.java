package com.qiniu.android.utils;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.BaseTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
public class GZipTest extends BaseTest {

    @Test
    public void testGZipString(){

        String string = null;

        byte[] gzip = GZipUtil.gZip(string);
        assertTrue(gzip == null);

        string = "";
        gzip = GZipUtil.gZip(string);
        assertTrue(Arrays.equals(gzip, string.getBytes()));


        string = "ABCDEFG";
        gzip = GZipUtil.gZip(string);

        byte[] gUnzip = GZipUtil.gUnzip(gzip);
        String stringGUnzip = new String(gUnzip);

        assertTrue(string.equals(stringGUnzip));
    }

    @Test
    public void testGZipByte(){

        byte[] bytes = null;

        byte[] gzip = GZipUtil.gZip(bytes);
        assertTrue(gzip == null);

        bytes = new byte[0];
        gzip = GZipUtil.gZip(bytes);
        assertTrue(Arrays.equals(bytes, gzip));


        String string = "ABCDEFG";
        bytes = string.getBytes();
        gzip = GZipUtil.gZip(bytes);

        byte[] gUnzip = GZipUtil.gUnzip(gzip);
        String stringGUnzip = new String(gUnzip);

        assertTrue(string.equals(stringGUnzip));
    }
}
