package com.qiniu.android;

import junit.framework.TestCase;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by yangsen on 2020/5/26
 */
public class BaseTest extends TestCase {

    private long maxWaitTimestamp = 0;
    /**
     * waitCondition: 等待条件
     * maxWaitTime: 等待最大时长 单位-秒
     */
    public void wait(WaitConditional waitConditional, float maxWaitTime){

        WaitConditional waitConditionalP = waitConditional;
        if (waitConditionalP == null){
            waitConditionalP = new WaitCondition();
        }

        this.maxWaitTimestamp = new Date().getTime() + (long)(maxWaitTime * 1000);
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


    public interface WaitConditional {
        boolean shouldWait();
    }

    public static class WaitCondition implements WaitConditional {
        public boolean shouldWait = true;
        public boolean shouldWait(){
            return shouldWait;
        };
    }


    private void notestWait(){

        long waitTime = 5;

        long startTimestamp = new Date().getTime();

        WaitCondition waitCondition = new WaitCondition();
        waitCondition.shouldWait = true;
        wait(waitCondition, waitTime);

        long endTimestamp = new Date().getTime();

        assertTrue(((startTimestamp + waitTime*1000) < endTimestamp));
    }
}
