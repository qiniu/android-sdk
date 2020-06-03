package com.qiniu.android;


import android.test.InstrumentationTestCase;
import android.util.Log;

import com.qiniu.android.common.AutoZone;
import com.qiniu.android.common.Config;
import com.qiniu.android.common.FixedZone;
import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.dns.DnsCacheKey;
import com.qiniu.android.http.dns.DnsPrefetcher;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.utils.AndroidNetwork;
import com.qiniu.android.utils.UrlSafeBase64;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Base64;
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
        dnsPrefetcher.checkAndPrefetchDnsIfNeed(fixedZone, TestConfig.token_z0);

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
                    boolean isSuccess = dnsPrefetcher.checkAndPrefetchDnsIfNeed(zone, TestConfig.token_z0);
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

        assertTrue((param.completeCount == param.successCount));
    }


    private static class TestParam{
        int count = 100;
        int successCount = 0;
        int completeCount = 0;
    }
}
