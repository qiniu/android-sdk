package com.qiniu.android;

import com.qiniu.android.utils.GZipUtil;
import java.util.Arrays;

public class GZipTest extends BaseTest {

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
