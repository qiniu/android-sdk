package com.qiniu.android.utils;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.TempFile;
import com.qiniu.android.common.Constants;
import com.qiniu.android.storage.Configuration;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

@RunWith(AndroidJUnit4.class)
public class EtagTest {

    @Test
    public void testData() {
        String m = Etag.data(new byte[0]);
        Assert.assertEquals("Fto5o-5ea0sNMlW_75VgGJCv2AcJ", m);

        try {
            String etag = Etag.data("etag".getBytes(Constants.UTF_8));
            Assert.assertEquals("FpLiADEaVoALPkdb8tJEJyRTXoe_", etag);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testFile() throws IOException {
        File f = TempFile.createFile(1024);
        Assert.assertEquals("FhHnGzB75K2JC4YOzKDMLEiaeSKm", Etag.file(f));
        TempFile.remove(f);
        f = TempFile.createFile(4 * 1024);
        Assert.assertEquals("FuPHVcYFMpfuoCTDGF5PCjMY9xxu", Etag.file(f));
        TempFile.remove(f);
        f = TempFile.createFile(5 * 1024);
        Assert.assertEquals("lkr1cErNyp23IdWan82rufDn3dzT", Etag.file(f));
        TempFile.remove(f);
        f = TempFile.createFile(8 * 1024);
        Assert.assertEquals("lkRgUHWNADyQ0TRirdqoS7UWFql4", Etag.file(f));
        TempFile.remove(f);
        f = TempFile.createFile(9 * 1024);
        Assert.assertEquals("lvlmp343GVuq367WF4XTMetchhid", Etag.file(f));
        TempFile.remove(f);
    }

    @Test
    public void testLongToInt() {
        long len = 2323435710l;
        int b = (int) ((len + Configuration.BLOCK_SIZE - 1) / Configuration.BLOCK_SIZE);
        Assert.assertEquals("不应该溢出", 554, b);
        int a = (int) (len + Configuration.BLOCK_SIZE - 1) / Configuration.BLOCK_SIZE;
        Assert.assertNotSame("预计会溢出", 554, a);
    }
}
