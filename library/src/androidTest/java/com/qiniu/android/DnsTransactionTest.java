package com.qiniu.android;

import com.qiniu.android.common.AutoZone;
import com.qiniu.android.common.Zone;
import com.qiniu.android.http.dns.DnsPrefetchTransaction;
import com.qiniu.android.storage.UpToken;


/**
 * Created by yangsen on 2020/6/9
 */
public class DnsTransactionTest extends BaseTest {

    private final int maxTestCount = 100;
    private int completeCount = 0;
    private int successCount = 0;

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


    public void test_CheckAndPrefetch(){

        completeCount = 0;
        successCount = 0;

        final Zone zone = new AutoZone();
        for (int i = 0; i < maxTestCount; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {

                    boolean isSuccess = DnsPrefetchTransaction.addDnsCheckAndPrefetchTransaction(zone, UpToken.parse(TestConfig.token_z0));
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
