package com.qiniu.android;

import android.test.AndroidTestCase;

import com.qiniu.android.storage.UpToken;

import junit.framework.Assert;

/**
 * Created by bailong on 15/6/1.
 */
public class TokenTest extends AndroidTestCase {
    public void testRight() {
        UpToken t = UpToken.parse(TestConfig.token);
        Assert.assertNotNull(t);
    }
}
