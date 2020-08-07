package com.qiniu.android;

import com.qiniu.android.utils.Wait;

public class WaitTest extends BaseTest {

    public int count = 0;

    public void testWait(){

        final Wait wait = new Wait();

        new Thread(new Runnable() {
            @Override
            public void run() {

                for (int i = 0; i < 1000; i++) {
                    count += 1;
                }

                wait.stopWait();
            }
        }).start();

        wait.startWait();

        assertEquals(count, 1000);
    }
}
