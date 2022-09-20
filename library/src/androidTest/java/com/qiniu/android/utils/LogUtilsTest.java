package com.qiniu.android.utils;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.BaseTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LogUtilsTest extends BaseTest {

    @Test
    public void testLog() {

        LogUtil.enableLog(true);
        LogUtil.setLogLevel(Log.VERBOSE);

        Throwable throwable = new Throwable();

        assertTrue(validLogCode(LogUtil.v("log")));
        assertTrue(validLogCode(LogUtil.v("v", "log")));
        assertTrue(validLogCode(LogUtil.v("v", "log", null)));
        assertTrue(validLogCode(LogUtil.v("v", "log", throwable)));

        assertTrue(validLogCode(LogUtil.d("log")));
        assertTrue(validLogCode(LogUtil.d("v", "log")));
        assertTrue(validLogCode(LogUtil.d("v", "log", null)));
        assertTrue(validLogCode(LogUtil.d("v", "log", throwable)));

        assertTrue(validLogCode(LogUtil.i("log")));
        assertTrue(validLogCode(LogUtil.i("v", "log")));
        assertTrue(validLogCode(LogUtil.i("v", "log", null)));
        assertTrue(validLogCode(LogUtil.i("v", "log", throwable)));

        assertTrue(validLogCode(LogUtil.w("log")));
        assertTrue(validLogCode(LogUtil.w("v", "log")));
        assertTrue(validLogCode(LogUtil.w("v", "log", null)));
        assertTrue(validLogCode(LogUtil.w("v", "log", throwable)));

        assertTrue(validLogCode(LogUtil.e("log")));
        assertTrue(validLogCode(LogUtil.e("v", "log")));
        assertTrue(validLogCode(LogUtil.e("v", "log", null)));
        assertTrue(validLogCode(LogUtil.e("v", "log", throwable)));

        LogUtil.enableLog(false);
    }

    private boolean validLogCode(int code) {
        return !(code == -10000 || code == -10001);
    }
}
