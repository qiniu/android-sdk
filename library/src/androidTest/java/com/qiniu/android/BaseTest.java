package com.qiniu.android;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Before;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by yangsen on 2020/5/26
 */
public class BaseTest {

    private long maxWaitTimestamp = 0;

    private Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
    }

    /**
     * waitCondition: 等待条件
     * maxWaitTime: 等待最大时长 单位-秒
     */
    public void wait(WaitConditional waitConditional, float maxWaitTime) {

        WaitConditional waitConditionalP = waitConditional;
        if (waitConditionalP == null) {
            waitConditionalP = new WaitCondition();
        }

        this.maxWaitTimestamp = new Date().getTime() + (long) (maxWaitTime * 1000);
        while (waitConditionalP.shouldWait()) {
            long currentTimestamp = new Date().getTime();
            if (currentTimestamp > maxWaitTimestamp) {
                break;
            }
            CountDownLatch pieceWaitSignal = new CountDownLatch(1);
            try {
                pieceWaitSignal.await(10, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected Context getContext() {
        return context;
    }

    public interface WaitConditional {
        boolean shouldWait();
    }

    public static class WaitCondition implements WaitConditional {
        public boolean shouldWait = true;

        public boolean shouldWait() {
            return shouldWait;
        }

        ;
    }


    private void notestWait() {

        long waitTime = 5;

        long startTimestamp = new Date().getTime();

        WaitCondition waitCondition = new WaitCondition();
        waitCondition.shouldWait = true;
        wait(waitCondition, waitTime);

        long endTimestamp = new Date().getTime();

        assertTrue(((startTimestamp + waitTime * 1000) < endTimestamp));
    }


    protected void fail(String message) {
        Assert.fail(message);
    }

    protected void assertTrue(boolean condition) {
        Assert.assertTrue(condition);
    }

    protected void assertTrue(String message, boolean condition) {
        Assert.assertTrue(message, condition);
    }

    protected void assertFalse(boolean condition) {
        Assert.assertFalse(condition);
    }

    protected void assertFalse(String message, boolean condition) {
        Assert.assertFalse(message, condition);
    }

    protected void assertNull(Object object) {
        Assert.assertNull(object);
    }

    protected void assertNull(String message, Object object) {
        Assert.assertNull(message, object);
    }

    protected void assertNotNull(Object object) {
        Assert.assertNotNull(object);
    }

    protected void assertNotNull(String message, Object object) {
        Assert.assertNotNull(message, object);
    }

    protected void assertEquals(Object expected, Object actual) {
        Assert.assertEquals(expected, actual);
    }

    protected void assertEquals(String message, Object expected, Object actual) {
        Assert.assertEquals(message, expected, actual);
    }

    protected void assertEquals(String message, long expected, long actual) {
        Assert.assertEquals(message, expected, actual);
    }

    protected void assertEquals(int expected, int actual) {
        Assert.assertEquals(expected, actual);
    }

    protected void assertEquals(String message, int expected, int actual) {
        Assert.assertEquals(message, expected, actual);
    }
}
