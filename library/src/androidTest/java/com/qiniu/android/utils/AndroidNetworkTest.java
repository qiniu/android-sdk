package com.qiniu.android.utils;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.BaseTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AndroidNetworkTest extends BaseTest {

    @Test
    public void testHostIP(){
        String ip = AndroidNetwork.getHostIP();
        assertTrue(ip != null);
    }

    @Test
    public void testNetworkType(){
        String type = AndroidNetwork.networkType(ContextGetter.applicationContext());
        assertTrue(type != null);
    }
}
