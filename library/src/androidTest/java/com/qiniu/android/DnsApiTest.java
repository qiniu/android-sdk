package com.qiniu.android;


import android.test.AndroidTestCase;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.qiniu.android.collect.Config;
import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.http.DnsPrefetcher;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.custom.DnsCacheKey;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.Recorder;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.storage.persistent.DnsCacheFile;
import com.qiniu.android.utils.AndroidNetwork;
import com.qiniu.android.utils.StringUtils;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jemy on 2019/8/20.
 */

public class DnsApiTest extends AndroidTestCase {
    private Configuration configuration = null;

    @Override
    protected void setUp() throws Exception {
        configuration = new Configuration.Builder().build();
    }

    public void testDns() throws Throwable {
        List<InetAddress> inetAddresses = null;
        try {
            inetAddresses = okhttp3.Dns.SYSTEM.lookup("upload.qiniup.com");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        Log.e("qiniutest", "InetAddress: " + inetAddresses.size());
        //超耗时过程
//        for (int i = 0; i < inetAddresses.size(); i++) {
//            Log.e("qiniutest", "InetAddress.getCanonicalHostName: " + inetAddresses.get(i).getCanonicalHostName());
//
//        }
        for (int i = 0; i < inetAddresses.size(); i++) {
            Log.e("qiniutest", "InetAddress.getHostAddress: " + inetAddresses.get(i).getHostAddress());
        }
    }


    public void testQueryDomain() {
        ZoneInfo info = null;

        DnsPrefetcher dnsPrefetcher = DnsPrefetcher.getDnsPrefetcher();
        try {
            info = dnsPrefetcher.init(TestConfig.uptoken_prefetch, configuration).getPreQueryZone();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        if (info == null) {
            Log.e("qiniutest: ", "null");
        }
        Log.e("qiniutest: ", info.toString());
        Log.e("qiniutest: ", info.upDomainsList.get(0));
    }


    public void testLocalDomain() {
        List<ZoneInfo> info = null;
        DnsPrefetcher dnsPrefetcher = DnsPrefetcher.getDnsPrefetcher();
        try {
            info = dnsPrefetcher.init(TestConfig.uptoken_prefetch, configuration).getLocalZone();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        if (info == null) {
            Log.e("qiniutest: ", "null");
        }
        for (int i = 0; i < info.size(); i++) {
            Log.e("qiniutest: ", info.get(i).toString());
        }

    }


    public void testLocalIp() {
        String s = AndroidNetwork.getHostIP();
        Log.e("qiniutest", s);
    }

    //test recover
    public void testRecoverCache() {

        Recorder recorder = null;
        try {
            recorder = new DnsCacheFile(Config.dnscacheDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String fileName = recorder.getFileName();
        if (fileName == null) {
            Log.e("qiniutest: ", "recover file is null ");
            return;
        }
        byte[] data = recorder.get(recorder.getFileName());
        if (data == null) {
            Log.e("qiniutest: ", "recover data is null ");
            return;
        }
        DnsPrefetcher.recoverDnsCache(data);


        ConcurrentHashMap<String, List<InetAddress>> map1 = DnsPrefetcher.getDnsPrefetcher().getConcurrentHashMap();
        if (map1.size() <= 0)
            return;
        for (String s : map1.keySet()) {
            Log.e("qiniutest: ", "uphost for cache: " + s);
            List<InetAddress> list1 = map1.get(s);
            for (InetAddress inetAddress :
                    list1) {
                Log.e("qiniutest: ", "ip for cache: " + inetAddress.getHostAddress());
            }
        }
    }

    int time = 0;
    final Object lock = new Object();


    public void testSerializable() {
        DnsCacheKey key = new DnsCacheKey("12321", "127.0.0.1", "akscope");
        Log.e("qiniutest", key.toString());
        DnsCacheKey key1 = DnsCacheKey.toCacheKey(key.toString());
        if (key1 == null) {
            return;
        }
        Log.e("qiniutest", key1.getCurrentTime() + ":" + key1.getLocalIp() + ":" + key1.getAkScope());

    }


}
