package com.qiniu.android.common;

import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CollectConfigTest {

    @Test
    public void testQuick(){
        Config.quick();
        assertTrue(Config.uploadThreshold == 1024);
        assertTrue(Config.interval == 2);
    }

    @Test
    public void testNormal(){
        Config.normal();
        assertTrue(Config.uploadThreshold == 4*1024);
        assertTrue(Config.interval == 10);
    }

    @Test
    public void testSlow(){
        Config.slow();
        assertTrue(Config.uploadThreshold == 150*1024);
        assertTrue(Config.interval == 300);
    }
}
