package com.qiniu.android.utils;

import android.os.Looper;

import com.qiniu.android.BaseTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class AsynTest extends BaseTest {

    private class TestParam {
        int maxCount;
        int completeCount;
        int successCount;
    }

    @Test
    public void testAsyncMainOnMainThread() {

        final TestParam testParam = new TestParam();
        testParam.maxCount = 100;
        testParam.completeCount = 0;
        testParam.successCount = 0;
        for (int i = 0; i < testParam.maxCount; i++) {
            final int i_p = i;
            AsyncRun.runInMain(new Runnable() {
                @Override
                public void run() {

                    if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                        testParam.successCount += 1;
                    }

                    String threadName = Thread.currentThread().getName();
                    LogUtil.d("thread name:" + threadName);

                    testParam.completeCount += 1;
                }
            });
        }

        WaitConditional waitConditional = new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (testParam.completeCount == testParam.maxCount) {
                    return false;
                } else {
                    return true;
                }
            }
        };

        wait(waitConditional, 5);

        LogUtil.i(String.format("success count: %d", testParam.successCount));
        assertTrue((testParam.successCount == testParam.maxCount));
    }

    @Test
    public void testAsyncMainOnOtherThread() {

        final TestParam testParam = new TestParam();
        testParam.maxCount = 10;
        testParam.completeCount = 0;
        testParam.successCount = 0;
        for (int i = 0; i < testParam.maxCount; i++) {
            final int i_p = i;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    AsyncRun.runInMain(new Runnable() {
                        @Override
                        public void run() {

                            if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                                testParam.successCount += 1;
                            }

                            String threadName = Thread.currentThread().getName();
                            LogUtil.d("thread name:" + threadName);

                            testParam.completeCount += 1;
                        }
                    });
                }
            }).start();
        }

        WaitConditional waitConditional = new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (testParam.completeCount == testParam.maxCount) {
                    return false;
                } else {
                    return true;
                }
            }
        };

        wait(waitConditional, 5);

        LogUtil.i(String.format("success count: %d", testParam.successCount));
        assertTrue((testParam.successCount == testParam.maxCount));
    }

    @Test
    public void testAsyncBg() {
        final TestParam testParam = new TestParam();
        testParam.maxCount = 100;
        testParam.completeCount = 0;
        testParam.successCount = 0;
        for (int i = 0; i < testParam.maxCount; i++) {
            final int i_p = i;
            AsyncRun.runInBack(new Runnable() {
                @Override
                public void run() {
                    String threadName = Thread.currentThread().getName();
                    if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
                        synchronized (this) {
                            testParam.successCount += 1;
                        }
                    }
                    synchronized (this) {
                        testParam.completeCount += 1;
                    }
                }
            });
        }

        WaitConditional waitConditional = new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (testParam.completeCount == testParam.maxCount) {
                    return false;
                } else {
                    return true;
                }
            }
        };

        wait(waitConditional, 5);

        LogUtil.i(String.format("success count: %d", testParam.successCount));
        assertTrue("pass", (testParam.successCount == testParam.maxCount));
    }
}
