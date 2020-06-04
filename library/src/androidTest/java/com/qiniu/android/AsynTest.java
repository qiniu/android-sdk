package com.qiniu.android;

import com.qiniu.android.utils.AsyncRun;
import com.qiniu.android.utils.LogUtil;


public class AsynTest extends BaseTest {

    private class TestParam {
        int maxCount;
        int completeCount;
        int successCount;
    }
    public void testAsynMain(){

        final TestParam testParam = new TestParam();
        testParam.maxCount = 100;
        testParam.completeCount = 0;
        testParam.successCount = 0;
        for (int i = 0; i < testParam.maxCount; i++) {
            final int i_p = i;
            AsyncRun.runInMain(new Runnable() {
                @Override
                public void run() {
                    String threadName = Thread.currentThread().getName();
                    if (threadName.equals("main")){
                        testParam.successCount += 1;
                    }

                    testParam.completeCount += 1;
                }
            });
        }

        WaitConditional waitConditional = new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (testParam.completeCount == testParam.maxCount){
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

    public void testAsynBg(){
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
                    if (!threadName.equals("main")){
                        synchronized (this){
                            testParam.successCount += 1;
                        }
                    }
                    synchronized (this){
                        testParam.completeCount += 1;
                    }
                }
            });
        }

        WaitConditional waitConditional = new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (testParam.completeCount == testParam.maxCount){
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
