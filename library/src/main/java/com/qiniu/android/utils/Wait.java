package com.qiniu.android.utils;

import java.util.concurrent.CountDownLatch;

public class Wait {

    final CountDownLatch completeSingle = new CountDownLatch(1);

    public void startWait(){
        while (completeSingle.getCount() > 0) {
            try {
                completeSingle.await();
                break;
            } catch (InterruptedException e) {
            }
        }
    }

    public void stopWait(){
        completeSingle.countDown();
    }

}
