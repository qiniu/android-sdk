package com.qiniu.android;

import com.qiniu.android.utils.LogUtil;

public class LogUtilsTest extends BaseTest {

    public void testLog(){

        Throwable throwable = new Throwable();

        assertTrue(LogUtil.v("log") > 0);
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

    }
}
