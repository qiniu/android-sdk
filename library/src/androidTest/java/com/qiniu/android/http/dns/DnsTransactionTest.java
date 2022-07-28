package com.qiniu.android.http.dns;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.TestConfig;
import com.qiniu.android.common.AutoZone;
import com.qiniu.android.common.Zone;
import com.qiniu.android.BaseTest;
import com.qiniu.android.storage.UpToken;

import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Created by yangsen on 2020/6/9
 */
@RunWith(AndroidJUnit4.class)
public class DnsTransactionTest extends BaseTest {

    private final int maxTestCount = 100;
    private int completeCount = 0;
    private int successCount = 0;

    @Test
    public void testLocalLoad(){

        completeCount = 0;
        successCount = 0;

        for (int i = 0; i < maxTestCount; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {

                    boolean isSuccess = DnsPrefetchTransaction.addDnsLocalLoadTransaction();
                    if (isSuccess){
                        successCount += 1;
                    }
                    completeCount += 1;
                }
            }).start();
        }

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (completeCount < maxTestCount) {
                    return true;
                } else {
                    return false;
                }
            }
        }, 60);

        assertTrue(successCount < 2);
    }

    @Test
    public void test_CheckAndPrefetch(){

        completeCount = 0;
        successCount = 0;

        final Zone zone = new AutoZone();
        for (int i = 0; i < maxTestCount; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {

                    boolean isSuccess = DnsPrefetchTransaction.addDnsCheckAndPrefetchTransaction(zone, UpToken.parse(TestConfig.token_z0));
                    synchronized (this) {
                        if (isSuccess) {
                            successCount += 1;
                        }
                        completeCount += 1;
                    }
                }
            }).start();
        }

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (completeCount < maxTestCount) {
                    return true;
                } else {
                    return false;
                }
            }
        }, 60);

        assertTrue("successCount:" + successCount, successCount < 3);
    }

    @Test
    public void testCheckWhetherCachedValid(){

        completeCount = 0;
        successCount = 0;

        final Zone zone = new AutoZone();
        for (int i = 0; i < maxTestCount; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {

                    boolean isSuccess = DnsPrefetchTransaction.setDnsCheckWhetherCachedValidTransactionAction();
                    if (isSuccess){
                        successCount += 1;
                    }
                    completeCount += 1;
                }
            }).start();
        }

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (completeCount < maxTestCount) {
                    return true;
                } else {
                    return false;
                }
            }
        }, 60);

        assertTrue(successCount < 2);
    }
}
