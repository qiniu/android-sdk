package com.qiniu.android.utils;

import java.util.concurrent.CountDownLatch;

/**
 * wait
 *
 * @hidden
 */
public class Wait {

    final CountDownLatch completeSingle = new CountDownLatch(1);

    /**
     * 构造函数
     */
    public Wait() {
    }

    /**
     * 开始等待
     */
    public void startWait() {
        while (completeSingle.getCount() > 0) {
            try {
                completeSingle.await();
                break;
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * 停止等待
     */
    public void stopWait() {
        completeSingle.countDown();
    }

}
