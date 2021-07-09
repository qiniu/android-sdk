package com.qiniu.android;

import android.util.Log;

import com.qiniu.android.utils.LogUtil;

public class LogUtilsTest extends BaseTest {

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
        return code > 0 || code == -10000 || code == -10001;
    }
}
