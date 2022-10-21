package com.qiniu.android.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.TestConfig;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

/**
 * Created by bailong on 15/6/1.
 */
@RunWith(AndroidJUnit4.class)
public class TokenTest {

    @Test
    public void testRight() {

        UpToken t = UpToken.parse(TestConfig.commonToken);

        assertTrue(!UpToken.isInvalid(t));
        assertTrue("token isValidForDuration error", t.isValidForDuration(5*60));
        assertTrue("token isValidBeforeDate error", t.isValidBeforeDate(new Date()));
        assertTrue(t.toString() != null);
        assertNotSame(t, null);
    }

    public void testEmpty() {
        UpToken t = UpToken.parse(null);
        assertEquals(t, null);

        t = UpToken.parse("");
        assertEquals(t, null);

        t = UpToken.parse("1:2:3");
        assertEquals(t, null);
    }

    @Test
    public void testReturnUrl() {
        UpToken t = UpToken.parse("QWYn5TFQsLLU1pL5MFEmX3s5DmHdUThav9WyOWOm:1jLiztn4plVyeB8Hie1ryO5z9uo=:eyJzY29wZSI6InB5c2RrIiwiZGVhZGxpbmUiOjE0MzM0ODM5MzYsInJldHVyblVybCI6Imh0dHA6Ly8xMjcuMC4wLjEvIn0=");
        assertTrue(t.hasReturnUrl());
    }

}
