package com.qiniu.android.common;

import android.test.AndroidTestCase;

import com.qiniu.android.TestConfig;

import junit.framework.Assert;

import java.util.concurrent.CountDownLatch;

/**
 * Created by long on 2016/9/30.
 */

public class AutoZoneTest extends AndroidTestCase {
    private String ak = TestConfig.ak;
    private String bkt = "javasdk";

    public void testHttp() {
        AutoZone zone = new AutoZone(false, null);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        zone.preQueryIndex(new AutoZone.ZoneIndex(ak, bkt), new Zone.QueryHandler() {
            @Override
            public void onSuccess() {
                countDownLatch.countDown();
            }

            @Override
            public void onFailure(int reason) {
                countDownLatch.countDown();
                fail();
            }
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        AutoZone.ZoneInfo zoneInfo = zone.zoneInfo(ak, bkt);
        assertEquals(zoneInfo.upHost, "http://upload.qiniu.com");
        assertEquals(zoneInfo.upBackup, "http://up.qiniu.com");
        assertEquals(zoneInfo.upHttps, "https://up.qbox.me");
    }

    public void testHttpFail() {
        AutoZone zone = new AutoZone(false, null);
        AutoZone.ZoneInfo zoneInfo = zone.zoneInfo(ak + "_not_be_ak", bkt);
        assertNull(zoneInfo);
    }

    public void testSplitE() {
        String s1 = "bkt:key";
        String s2 = "bkt";
        Assert.assertEquals(s1.split(":")[0], s2.split(":")[0]);
    }

    public void testC1() {
        AutoZone autoZone = new AutoZone(false, null);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        autoZone.preQueryIndex(new AutoZone.ZoneIndex(ak, bkt), new Zone.QueryHandler() {
            @Override
            public void onSuccess() {
                countDownLatch.countDown();
            }

            @Override
            public void onFailure(int reason) {
                countDownLatch.countDown();
                fail();
            }
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        AutoZone.ZoneInfo info = autoZone.zoneInfo(ak, bkt);
        System.out.println("zone0: " + info.toString());

        AutoZone.ZoneInfo info2 = autoZone.zoneInfo(ak, bkt);
        Assert.assertSame(info, info2);

    }
}
