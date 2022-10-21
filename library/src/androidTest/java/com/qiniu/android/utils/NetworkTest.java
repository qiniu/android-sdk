package com.qiniu.android.utils;

import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by bailong on 16/9/7.
 */
@RunWith(AndroidJUnit4.class)
public class NetworkTest {

    @Test
    public void testConnected() {
        boolean stat = AndroidNetwork.isNetWorkReady();
        assertTrue(stat);
    }
}
