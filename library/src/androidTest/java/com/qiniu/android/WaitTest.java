package com.qiniu.android;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.utils.Wait;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WaitTest extends BaseTest {

    public int count = 0;

    @Test
    public void testWait() {

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
