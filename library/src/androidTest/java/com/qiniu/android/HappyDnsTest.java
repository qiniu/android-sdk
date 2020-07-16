package com.qiniu.android;

import com.qiniu.android.http.dns.HappyDns;
import com.qiniu.android.http.dns.IDnsNetworkAddress;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yangsen on 2020/6/8
 */
public class HappyDnsTest extends BaseTest {

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
