package com.qiniu.android;

import android.test.AndroidTestCase;

import com.qiniu.android.common.Config;

public class CollectConfigTest extends AndroidTestCase {

    public void testQuick(){
        Config.quick();
        assertTrue(Config.uploadThreshold == 1024);
        assertTrue(Config.interval == 2);
    }

    public void testNormal(){
        Config.normal();
        assertTrue(Config.uploadThreshold == 4*1024);
        assertTrue(Config.interval == 10);
    }

    public void testSlow(){
        Config.slow();
        assertTrue(Config.uploadThreshold == 150*1024);
        assertTrue(Config.interval == 300);
    }
}
