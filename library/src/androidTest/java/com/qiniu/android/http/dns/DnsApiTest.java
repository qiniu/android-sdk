package com.qiniu.android.http.dns;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.qiniu.android.TestConfig;
import com.qiniu.android.common.AutoZone;
import com.qiniu.android.common.FixedZone;
import com.qiniu.android.BaseTest;
import com.qiniu.android.storage.UpToken;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * Created by jemy on 2019/8/20.
 */

@RunWith(AndroidJUnit4.class)
public class DnsApiTest extends BaseTest {

    @Override @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testLocalLoad() {

        final String host = "uplog.qbox.me";
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

    private void testRecover(){
        final String host = "uplog.qbox.me";

        final DnsPrefetcher dnsPrefetcher = DnsPrefetcher.getInstance();
        dnsPrefetcher.recoverCache();

        List<IDnsNetworkAddress> addressList = dnsPrefetcher.getInetAddressByHost(host);
        assertTrue(addressList.size() > 0);

        dnsPrefetcher.invalidNetworkAddress(addressList.get(0));
    }

    @Test
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

    @Test
    public void testMutiThreadPrefetch(){

        final AutoZone zone = new AutoZone();
        final TestParam param = new TestParam();

        for (int i = 0; i < param.count; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean isSuccess = DnsPrefetchTransaction.addDnsCheckAndPrefetchTransaction(zone, UpToken.parse(TestConfig.token_z0));
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
