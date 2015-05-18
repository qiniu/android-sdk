package com.qiniu.android;

import android.test.AndroidTestCase;

import com.qiniu.android.common.Constants;
import com.qiniu.android.utils.Etag;

import junit.framework.Assert;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class EtagTest extends AndroidTestCase {
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

    public void testFile() throws IOException {
        File f = TempFile.createFile(1024);
        Assert.assertEquals("Foyl8onxBLWeRLL5oItRJphv6i4b", Etag.file(f));
        TempFile.remove(f);
        f = TempFile.createFile(4 * 1024);
        Assert.assertEquals("FicHOveBNs5Kn9d74M3b9tI4D-8r", Etag.file(f));
        TempFile.remove(f);
        f = TempFile.createFile(5 * 1024);
        Assert.assertEquals("lg-Eb5KFCuZn-cUfj_oS2PPOU9xy", Etag.file(f));
        TempFile.remove(f);
        f = TempFile.createFile(8 * 1024);
        Assert.assertEquals("lkSKZOMToDp-EqLDVuT1pyjQssl-", Etag.file(f));
        TempFile.remove(f);
        f = TempFile.createFile(9 * 1024);
        Assert.assertEquals("ljgVjMtyMsOgIySv79U8Qz4TrUO4", Etag.file(f));
        TempFile.remove(f);

    }
}
