package com.qiniu.android;

import android.util.Log;

import com.qiniu.android.utils.AsyncRun;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AsynTest extends TestCase {

    final CountDownLatch signal = new CountDownLatch(1);

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

                    if (testParam.completeCount == testParam.maxCount){
                        signal.countDown();
                    }
                }
            });
        }

        try {
            signal.await(6000, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.i("Asyn test", String.format("success count: %d", testParam.successCount));
        assertTrue("pass", (testParam.successCount == testParam.maxCount));
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
                    if (threadName.equals("main") == false){
                        testParam.successCount += 1;
                    }
//                    Log.i("Asyn test", String.format("== thread name: %d", threadName));
                    testParam.completeCount += 1;

                    if (testParam.completeCount == testParam.maxCount){
                        signal.countDown();
                    }
                }
            });
        }

        try {
            signal.await(6000, TimeUnit.SECONDS); // wait for callback
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.i("Asyn test", String.format("success count: %d", testParam.successCount));
        assertTrue("pass", (testParam.successCount == testParam.maxCount));
    }
}
