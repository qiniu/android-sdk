package com.qiniu.android;

import com.qiniu.android.utils.AndroidNetwork;

import junit.framework.TestCase;

/**
 * Created by bailong on 16/9/7.
 */
public class NetworkTest extends TestCase {
    public void testConnected() {
        boolean stat = AndroidNetwork.isNetWorkReady();
        assertTrue(stat);
    }
}
