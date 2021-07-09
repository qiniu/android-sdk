package com.qiniu.android;

import android.util.Log;

import com.qiniu.android.utils.LogUtil;

public class LogUtilsTest extends BaseTest {

    public void testLog(){

        LogUtil.enableLog(true);
        LogUtil.setLogLevel(Log.VERBOSE);

        Throwable throwable = new Throwable();

        int ret = LogUtil.v("log");
        assertTrue("log ret:" + ret, ret > 0);
        assertTrue(LogUtil.v("v","log") > 0);
        assertTrue(LogUtil.v("v","log", null) > 0);
        assertTrue(LogUtil.v("v","log", throwable) > 0);

        assertTrue(LogUtil.d("log") > 0);
        assertTrue(LogUtil.d("v","log") > 0);
        assertTrue(LogUtil.d("v","log", null) > 0);
        assertTrue(LogUtil.d("v","log", throwable) > 0);

        assertTrue(LogUtil.i("log") > 0);
        assertTrue(LogUtil.i("v","log") > 0);
        assertTrue(LogUtil.i("v","log", null) > 0);
        assertTrue(LogUtil.i("v","log", throwable) > 0);

        assertTrue(LogUtil.w("log") > 0);
        assertTrue(LogUtil.w("v","log") > 0);
        assertTrue(LogUtil.w("v","log", null) > 0);
        assertTrue(LogUtil.w("v","log", throwable) > 0);

        assertTrue(LogUtil.e("log") > 0);
        assertTrue(LogUtil.e("v","log") > 0);
        assertTrue(LogUtil.e("v","log", null) > 0);
        assertTrue(LogUtil.e("v","log", throwable) > 0);

        LogUtil.enableLog(false);
    }
}
