package com.qiniu.android.utils;

import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ContextTest {

    @Test
    public void testEncode() {
        Context c = ContextGetter.applicationContext();
        assertNotNull(c);
    }
}
