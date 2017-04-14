package com.qiniu.android;

import android.content.Context;
import android.test.AndroidTestCase;

import com.qiniu.android.utils.ContextGetter;

public class ContextTest extends AndroidTestCase {
    public void testEncode() {
        Context c = ContextGetter.applicationContext();
        assertNotNull(c);
    }
}
