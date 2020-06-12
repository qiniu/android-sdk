package com.qiniu.android;

import android.test.AndroidTestCase;

import com.qiniu.android.storage.UpToken;
import com.qiniu.android.utils.LogUtil;

import junit.framework.Assert;

import java.util.Arrays;

/**
 * Created by bailong on 15/6/1.
 */
public class TokenTest extends AndroidTestCase {
    public void testRight() {
        UpToken t = UpToken.parse(TestConfig.token_z0);
        Assert.assertNotSame(t, null);
    }

    public void testEmpty() {
        UpToken t = UpToken.parse(null);
        Assert.assertEquals(t, null);

        t = UpToken.parse("");
        Assert.assertEquals(t, null);
    }

    public void testReturnUrl() {
        UpToken t = UpToken.parse("QWYn5TFQsLLU1pL5MFEmX3s5DmHdUThav9WyOWOm:1jLiztn4plVyeB8Hie1ryO5z9uo=:eyJzY29wZSI6InB5c2RrIiwiZGVhZGxpbmUiOjE0MzM0ODM5MzYsInJldHVyblVybCI6Imh0dHA6Ly8xMjcuMC4wLjEvIn0=");
        Assert.assertTrue(t.hasReturnUrl());
    }
}
