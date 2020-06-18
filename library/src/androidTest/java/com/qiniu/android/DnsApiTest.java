package com.qiniu.android;

import com.qiniu.android.common.AutoZone;
import com.qiniu.android.common.FixedZone;
import com.qiniu.android.http.dns.DnsPrefetcher;
import com.qiniu.android.storage.UpToken;

import java.util.List;

/**
 * Created by jemy on 2019/8/20.
 */

public class DnsApiTest extends BaseTest {


    public void testLocalLoad() {

        final String host = "upload.qiniup.com";
        final DnsPrefetcher dnsPrefetcher = DnsPrefetcher.getInstance();
        dnsPrefetcher.localFetch();

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                List list = dnsPrefetcher.getInetAddressByHost(host);
                if (list == null || list.size() == 0){
                    return true;
                } else {
                    return false;
                }
            }
        }, 60);

        List addressList = dnsPrefetcher.getInetAddressByHost(host);
        assertTrue(addressList.size() > 0);
    }

    public void testRecover(){
        final String host = "upload.qiniup.com";

        final DnsPrefetcher dnsPrefetcher = DnsPrefetcher.getInstance();
        dnsPrefetcher.recoverCache();

        List addressList = dnsPrefetcher.getInetAddressByHost(host);
        assertTrue(addressList.size() > 0);
    }


    public void testPreFetch() {

        final String host = "upload.qiniup.com";
        FixedZone fixedZone = new FixedZone(new String[]{host});

        final DnsPrefetcher dnsPrefetcher = DnsPrefetcher.getInstance();
        dnsPrefetcher.checkAndPrefetchDnsIfNeed(fixedZone, UpToken.parse(TestConfig.token_z0));

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                List list = dnsPrefetcher.getInetAddressByHost(host);
                if (list == null || list.size() == 0){
                    return true;
                } else {
                    return false;
                }
            }
        }, 60);

        List addressList = dnsPrefetcher.getInetAddressByHost(host);
        assertTrue(addressList.size() > 0);
    }

    public void testMutiThreadPrefetch(){

        final AutoZone zone = new AutoZone();
        final DnsPrefetcher dnsPrefetcher = DnsPrefetcher.getInstance();

        final TestParam param = new TestParam();

        for (int i = 0; i < param.count; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean isSuccess = dnsPrefetcher.checkAndPrefetchDnsIfNeed(zone, UpToken.parse(TestConfig.token_z0));
                    synchronized (this){
                        if (isSuccess){
                            param.successCount += 1;
                        }
                        param.completeCount += 1;
                    }
                }
            }).start();
        }

        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                if (param.completeCount != param.count){
                    return true;
                } else {
                    return false;
                }
            }
        }, 60);

        assertTrue((param.successCount <= 1));
    }


    private static class TestParam{
        int count = 100;
        int successCount = 0;
        int completeCount = 0;
    }
}
