package com.qiniu.android;

import com.qiniu.android.utils.AndroidNetwork;
import com.qiniu.android.utils.ContextGetter;
import com.qiniu.android.utils.LogUtil;

public class AndroidNetworkTest extends BaseTest {

    public void testHostIP(){
        String ip = AndroidNetwork.getHostIP();
        assertTrue(ip != null);
    }

    public void testNetworkType(){
        String type = AndroidNetwork.networkType(ContextGetter.applicationContext());
        assertTrue(type != null);
    }

    public void testMobileDbm(){
        int dbm = AndroidNetwork.getMobileDbm();
        LogUtil.i(dbm + "");
//        assertTrue(dbm != -1);
    }

}
