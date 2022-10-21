package com.qiniu.android.http.dns;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.BaseTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yangsen on 2020/6/8
 */
@RunWith(AndroidJUnit4.class)
public class HappyDnsTest extends BaseTest {

    @Test
    public void testDns(){

        HappyDns happyDns = new HappyDns();
        List<IDnsNetworkAddress> inetAddressList = new ArrayList<>();

        try {
            inetAddressList = happyDns.lookup("qiniu.com");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        assertTrue(inetAddressList.size() > 0);
    }
}
